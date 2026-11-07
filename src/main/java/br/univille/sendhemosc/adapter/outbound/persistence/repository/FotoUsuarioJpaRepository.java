package br.univille.sendhemosc.adapter.outbound.persistence.repository;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.FotoUsuarioEntity;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acesso JPA as fotos de perfil.
 */
public interface FotoUsuarioJpaRepository extends JpaRepository<FotoUsuarioEntity, Long> {

    /**
     * Momento em que a foto foi gravada, sem trazer os bytes da imagem.
     *
     * @param usuarioId conta dona da foto
     * @return o momento, vazio quando a conta nao tem foto
     */
    @Query("SELECT f.atualizadoEm FROM FotoUsuarioEntity f WHERE f.usuarioId = :usuarioId")
    Optional<LocalDateTime> momentoDaFoto(@Param("usuarioId") Long usuarioId);

    /**
     * Apaga a foto da conta.
     *
     * @param usuarioId conta dona da foto
     * @return quantas fotos foram apagadas: um, ou zero quando nao havia
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM FotoUsuarioEntity f WHERE f.usuarioId = :usuarioId")
    int apagarDaConta(@Param("usuarioId") Long usuarioId);
}
