package br.univille.sendhemosc.adapter.outbound.persistence.repository;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.EstoqueEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso JPA ao estoque de hemocomponentes.
 */
public interface EstoqueJpaRepository extends JpaRepository<EstoqueEntity, Long> {

    Optional<EstoqueEntity> findByTipoSanguineo(String tipoSanguineo);
}
