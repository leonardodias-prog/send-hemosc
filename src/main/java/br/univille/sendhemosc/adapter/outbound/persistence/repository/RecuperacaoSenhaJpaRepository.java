package br.univille.sendhemosc.adapter.outbound.persistence.repository;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.RecuperacaoSenhaEntity;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acesso JPA aos pedidos de recuperacao de senha.
 */
public interface RecuperacaoSenhaJpaRepository extends JpaRepository<RecuperacaoSenhaEntity, Long> {

    Optional<RecuperacaoSenhaEntity> findByTokenHash(String tokenHash);

    long countByUsuarioIdAndCriadoEmAfter(Long usuarioId, LocalDateTime desde);

    long countByCriadoEmAfter(LocalDateTime desde);

    @Query("SELECT MAX(r.criadoEm) FROM RecuperacaoSenhaEntity r WHERE r.usuarioId = :usuarioId")
    Optional<LocalDateTime> ultimoDaConta(@Param("usuarioId") Long usuarioId);

    /**
     * Fecha o pedido somente se ele ainda estiver aberto e dentro da validade.
     *
     * @param id pedido
     * @param quando momento do uso
     * @return 1 se esta chamada fechou o pedido, 0 se ele ja estava fechado ou vencido
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE RecuperacaoSenhaEntity r SET r.usadoEm = :quando "
            + "WHERE r.id = :id AND r.usadoEm IS NULL AND r.expiraEm > :quando")
    int usar(@Param("id") Long id, @Param("quando") LocalDateTime quando);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE RecuperacaoSenhaEntity r SET r.usadoEm = :quando "
            + "WHERE r.usuarioId = :usuarioId AND r.usadoEm IS NULL")
    int invalidarAbertos(@Param("usuarioId") Long usuarioId, @Param("quando") LocalDateTime quando);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM RecuperacaoSenhaEntity r WHERE r.criadoEm < :limite")
    int apagarAnterioresA(@Param("limite") LocalDateTime limite);
}
