package br.univille.sendhemosc.adapter.outbound.persistence.repository;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.AuditoriaEntity;
import java.util.Collection;
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

    /**
     * Registros filtrados, do mais novo para o mais antigo.
     *
     * @param todasAcoes true dispensa o filtro de acao (a lista vazia nao serve em "in")
     * @param acoes acoes aceitas quando o filtro vale
     * @param semBusca true dispensa a busca por texto
     * @param busca trecho em minusculas entre curingas
     * @param paginacao trecho pedido
     * @return o trecho
     */
    @Query("""
            select a from AuditoriaEntity a
             where (:todasAcoes = true or a.acao in :acoes)
               and (:semBusca = true or lower(a.detalhe) like :busca or lower(a.usuarioEmail) like :busca)
             order by a.ocorridoEm desc, a.id desc
            """)
    Page<AuditoriaEntity> buscar(@Param("todasAcoes") boolean todasAcoes, @Param("acoes") Collection<String> acoes,
                                 @Param("semBusca") boolean semBusca, @Param("busca") String busca,
                                 Pageable paginacao);
}
