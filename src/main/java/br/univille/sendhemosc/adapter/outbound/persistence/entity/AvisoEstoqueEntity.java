package br.univille.sendhemosc.adapter.outbound.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Configuracao do aviso interno de estoque. Existe uma unica linha, criada pela migration.
 */
@Entity
@Table(name = "aviso_estoque")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvisoEstoqueEntity {

    @Id
    private Long id;

    @Column(name = "hora_relatorio", nullable = false)
    private int horaRelatorio;

    @Column(name = "ultimo_relatorio_em")
    private LocalDate ultimoRelatorioEm;

    @Column(name = "atualizado_por", length = 180)
    private String atualizadoPor;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;
}
