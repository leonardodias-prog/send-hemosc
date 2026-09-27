package br.univille.sendhemosc.adapter.outbound.persistence.repository;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.UsuarioEntity;
import br.univille.sendhemosc.domain.enums.PerfilUsuario;
import br.univille.sendhemosc.domain.enums.SituacaoUsuario;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acesso JPA as contas de usuario.
 */
public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Long> {

    Optional<UsuarioEntity> findByEmailIgnoreCase(String email);

    Optional<UsuarioEntity> findByTokenAprovacao(String tokenAprovacao);

    Optional<UsuarioEntity> findByTokenAprovacaoAndSituacao(String tokenAprovacao, SituacaoUsuario situacao);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPerfil(PerfilUsuario perfil);

    List<UsuarioEntity> findBySituacaoOrderByCriadoEmDesc(SituacaoUsuario situacao);

    List<UsuarioEntity> findByPerfilAndSituacao(PerfilUsuario perfil, SituacaoUsuario situacao);

    long countByPerfilAndSituacao(PerfilUsuario perfil, SituacaoUsuario situacao);

    List<UsuarioEntity> findAllByOrderByCriadoEmDesc();

    @Query("SELECT MAX(u.aprovacaoAvisadaEm) FROM UsuarioEntity u")
    Optional<LocalDateTime> ultimoAvisoDeCadastros();

    /**
     * Marca como avisados os pendentes que ainda nao entraram em nenhum aviso.
     *
     * @param quando momento do aviso, que fica gravado em cada cadastro marcado
     * @param situacao situacao pendente
     * @return quantos cadastros esta chamada marcou
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE UsuarioEntity u SET u.aprovacaoAvisadaEm = :quando "
            + "WHERE u.situacao = :situacao AND u.aprovacaoAvisadaEm IS NULL")
    int marcarPendentesComoAvisados(@Param("quando") LocalDateTime quando,
                                    @Param("situacao") SituacaoUsuario situacao);

    List<UsuarioEntity> findBySituacaoAndAprovacaoAvisadaEmOrderByCriadoEmAsc(SituacaoUsuario situacao,
                                                                            LocalDateTime aprovacaoAvisadaEm);
}
