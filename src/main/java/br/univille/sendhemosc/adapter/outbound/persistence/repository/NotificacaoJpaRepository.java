package br.univille.sendhemosc.adapter.outbound.persistence.repository;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.NotificacaoEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acesso JPA ao historico de convocacoes.
 */
public interface NotificacaoJpaRepository extends JpaRepository<NotificacaoEntity, Long> {

    /**
     * Doadores cuja convocacao mais recente para o tipo informado foi enviada antes da data limite
     * e que ainda nao registraram comparecimento.
     *
     * @param tipoSanguineo sigla do tipo sanguineo alvo
     * @param limite data limite de corte
     * @return identificadores dos doadores a reconvocar
     */
    @Query("""
            SELECT n.doadorId FROM NotificacaoEntity n
             WHERE n.tipoSanguineoAlvo = :tipoSanguineo
               AND n.status = br.univille.sendhemosc.domain.enums.StatusNotificacao.ENVIADA
               AND n.compareceuEm IS NULL
             GROUP BY n.doadorId
            HAVING MAX(n.enviadaEm) < :limite
            """)
    List<Long> buscarPendentesDeReenvio(@Param("tipoSanguineo") String tipoSanguineo,
                                        @Param("limite") LocalDateTime limite);

    @Query("SELECT COALESCE(MAX(n.tentativa), 0) FROM NotificacaoEntity n "
            + "WHERE n.doadorId = :doadorId AND n.tipoSanguineoAlvo = :tipoSanguineo")
    int maiorTentativa(@Param("doadorId") Long doadorId, @Param("tipoSanguineo") String tipoSanguineo);

    /**
     * Fecha as convocacoes que o doador tinha em aberto, marcando a data da doacao.
     *
     * @param doadorId quem doou
     * @param dataDoacao data da doacao
     * @return quantas convocacoes foram fechadas
     */
    @Modifying
    @Query("""
            UPDATE NotificacaoEntity n
               SET n.compareceuEm = :dataDoacao,
                   n.status = br.univille.sendhemosc.domain.enums.StatusNotificacao.COMPARECEU
             WHERE n.doadorId = :doadorId
               AND n.status = br.univille.sendhemosc.domain.enums.StatusNotificacao.ENVIADA
               AND n.compareceuEm IS NULL
            """)
    int marcarComparecimento(@Param("doadorId") Long doadorId,
                             @Param("dataDoacao") LocalDate dataDoacao);
}
