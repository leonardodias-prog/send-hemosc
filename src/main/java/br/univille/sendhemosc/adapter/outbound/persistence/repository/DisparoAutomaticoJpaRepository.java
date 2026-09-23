package br.univille.sendhemosc.adapter.outbound.persistence.repository;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.DisparoAutomaticoEntity;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acesso JPA a configuracao do disparo automatico.
 */
public interface DisparoAutomaticoJpaRepository extends JpaRepository<DisparoAutomaticoEntity, Long> {

    /**
     * Assume a primeira rodada, quando nenhuma aconteceu ainda.
     *
     * @param id linha da configuracao
     * @param agora inicio da rodada
     * @return 1 se esta chamada assumiu a rodada, 0 se outra ja tinha assumido
     */
    @Modifying
    @Query("UPDATE DisparoAutomaticoEntity d SET d.ultimaRodadaEm = :agora "
            + "WHERE d.id = :id AND d.ultimaRodadaEm IS NULL")
    int assumirPrimeiraRodada(@Param("id") Long id, @Param("agora") LocalDateTime agora);

    /**
     * Assume a rodada somente se a ultima registrada ainda e a que foi lida.
     *
     * @param id linha da configuracao
     * @param anterior ultima rodada que a verificacao enxergou
     * @param agora inicio da rodada
     * @return 1 se esta chamada assumiu a rodada, 0 se outra ja tinha assumido
     */
    @Modifying
    @Query("UPDATE DisparoAutomaticoEntity d SET d.ultimaRodadaEm = :agora "
            + "WHERE d.id = :id AND d.ultimaRodadaEm = :anterior")
    int assumirRodada(@Param("id") Long id, @Param("anterior") LocalDateTime anterior,
                      @Param("agora") LocalDateTime agora);

    @Modifying
    @Query("UPDATE DisparoAutomaticoEntity d SET d.ultimaRodadaResumo = :resumo WHERE d.id = :id")
    int registrarResumo(@Param("id") Long id, @Param("resumo") String resumo);
}
