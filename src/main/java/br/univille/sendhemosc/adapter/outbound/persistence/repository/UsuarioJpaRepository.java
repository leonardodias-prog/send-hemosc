package br.univille.sendhemosc.adapter.outbound.persistence.repository;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.UsuarioEntity;
import br.univille.sendhemosc.domain.enums.PerfilUsuario;
import br.univille.sendhemosc.domain.enums.SituacaoUsuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso JPA as contas de usuario.
 */
public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Long> {

    Optional<UsuarioEntity> findByEmailIgnoreCase(String email);

    Optional<UsuarioEntity> findByTokenAprovacao(String tokenAprovacao);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPerfil(PerfilUsuario perfil);

    List<UsuarioEntity> findBySituacaoOrderByCriadoEmDesc(SituacaoUsuario situacao);

    List<UsuarioEntity> findByPerfilAndSituacao(PerfilUsuario perfil, SituacaoUsuario situacao);

    long countByPerfilAndSituacao(PerfilUsuario perfil, SituacaoUsuario situacao);

    List<UsuarioEntity> findAllByOrderByCriadoEmDesc();
}
