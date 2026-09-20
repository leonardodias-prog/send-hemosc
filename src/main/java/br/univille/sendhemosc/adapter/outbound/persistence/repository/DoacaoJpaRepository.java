package br.univille.sendhemosc.adapter.outbound.persistence.repository;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.DoacaoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso JPA ao historico de doacoes.
 */
public interface DoacaoJpaRepository extends JpaRepository<DoacaoEntity, Long> {
}
