package br.univille.sendhemosc.adapter.outbound.persistence.repository;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.ConsentimentoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso JPA ao historico de consentimento dos doadores.
 */
public interface ConsentimentoJpaRepository extends JpaRepository<ConsentimentoEntity, Long> {
}
