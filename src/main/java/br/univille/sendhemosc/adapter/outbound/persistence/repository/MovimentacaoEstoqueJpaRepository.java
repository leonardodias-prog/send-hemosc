package br.univille.sendhemosc.adapter.outbound.persistence.repository;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.MovimentacaoEstoqueEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso JPA ao historico de movimentacao do estoque.
 */
public interface MovimentacaoEstoqueJpaRepository extends JpaRepository<MovimentacaoEstoqueEntity, Long> {

    List<MovimentacaoEstoqueEntity> findAllByOrderByOcorridoEmDescIdDesc(Pageable paginacao);
}
