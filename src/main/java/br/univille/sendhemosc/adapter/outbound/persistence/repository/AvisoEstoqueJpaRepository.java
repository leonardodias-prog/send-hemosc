package br.univille.sendhemosc.adapter.outbound.persistence.repository;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.AvisoEstoqueEntity;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acesso JPA a configuracao do aviso interno de estoque.
 */
public interface AvisoEstoqueJpaRepository extends JpaRepository<AvisoEstoqueEntity, Long> {

    /**
     * Assume o relatorio do dia somente se ele ainda nao saiu.
     *
     * @param id linha da configuracao
     * @param dia dia do relatorio
     * @return 1 se esta chamada assumiu o relatorio, 0 se o do dia ja tinha saido
     */
    @Modifying
    @Query("UPDATE AvisoEstoqueEntity a SET a.ultimoRelatorioEm = :dia "
            + "WHERE a.id = :id AND (a.ultimoRelatorioEm IS NULL OR a.ultimoRelatorioEm < :dia)")
    int assumirRelatorioDoDia(@Param("id") Long id, @Param("dia") LocalDate dia);
}
