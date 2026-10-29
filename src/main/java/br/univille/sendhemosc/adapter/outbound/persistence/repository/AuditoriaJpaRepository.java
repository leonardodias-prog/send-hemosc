package br.univille.sendhemosc.adapter.outbound.persistence.repository;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.AuditoriaEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acesso JPA ao registro de auditoria.
 */
public interface AuditoriaJpaRepository extends JpaRepository<AuditoriaEntity, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update AuditoriaEntity a set a.detalhe = replace(a.detalhe, :nome, :substituto)
            where a.acao in :acoes and a.detalhe like concat('%', :nome, '%')
            """)
    int substituirNoDetalhe(@Param("acoes") Collection<String> acoes, @Param("nome") String nome,
                            @Param("substituto") String substituto);

    Page<AuditoriaEntity> findAllByOrderByOcorridoEmDescIdDesc(Pageable paginacao);

    Page<AuditoriaEntity> findByAcaoOrderByOcorridoEmDescIdDesc(String acao, Pageable paginacao);

    @Query("select distinct a.acao from AuditoriaEntity a order by a.acao")
    List<String> listarAcoes();
}
