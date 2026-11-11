package br.univille.sendhemosc.adapter.outbound.persistence.repository;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.AvisoEstoqueEnvioEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso JPA ao registro dos avisos internos de estoque enviados.
 */
public interface AvisoEstoqueEnvioJpaRepository extends JpaRepository<AvisoEstoqueEnvioEntity, Long> {

    List<AvisoEstoqueEnvioEntity> findAllByOrderByEnviadoEmDescIdDesc(Pageable pagina);
}
