package br.univille.sendhemosc.adapter.outbound.persistence.repository;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.DoacaoEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acesso JPA ao historico de doacoes.
 */
public interface DoacaoJpaRepository extends JpaRepository<DoacaoEntity, Long> {

    boolean existsByDoadorIdAndDataDoacao(Long doadorId, LocalDate dataDoacao);

    @Query("SELECT d.dataDoacao FROM DoacaoEntity d WHERE d.dataDoacao BETWEEN :inicio AND :fim")
    List<LocalDate> listarDatas(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);
}
