package br.univille.sendhemosc.adapter.outbound.persistence.repository;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.AuditoriaEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso JPA ao registro de auditoria.
 */
public interface AuditoriaJpaRepository extends JpaRepository<AuditoriaEntity, Long> {

    List<AuditoriaEntity> findAllByOrderByOcorridoEmDesc(Pageable paginacao);
}
