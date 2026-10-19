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

    /**
     * Conta, por tipo sanguineo, as convocacoes enviadas no periodo e quantas delas fecharam
     * em comparecimento. Uma linha por convocacao enviada, nao por doador: quem recebeu duas
     * conta duas vezes, como em qualquer outro resumo de envio do sistema.
     *
     * @param inicio inicio do periodo, inclusive
     * @param fimExclusivo fim do periodo, exclusivo (o dia seguinte ao ultimo dia incluido)
     * @return tuplas (sigla do tipo, total convocado, total convertido)
     */
    @Query("""
            SELECT n.tipoSanguineoAlvo,
                   COUNT(n),
                   SUM(CASE WHEN n.status = br.univille.sendhemosc.domain.enums.StatusNotificacao.COMPARECEU
                            THEN 1L ELSE 0L END)
              FROM NotificacaoEntity n
             WHERE n.status IN (br.univille.sendhemosc.domain.enums.StatusNotificacao.ENVIADA,
                                 br.univille.sendhemosc.domain.enums.StatusNotificacao.COMPARECEU)
               AND n.enviadaEm >= :inicio AND n.enviadaEm < :fimExclusivo
             GROUP BY n.tipoSanguineoAlvo
            """)
    List<Object[]> contarConversaoPorTipo(@Param("inicio") LocalDateTime inicio,
                                          @Param("fimExclusivo") LocalDateTime fimExclusivo);
}
