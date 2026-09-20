package br.univille.sendhemosc.adapter.outbound.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Estoque de hemocomponentes por tipo sanguineo.
 */
@Entity
@Table(name = "estoque_hemocomponente")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstoqueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_sanguineo", nullable = false, length = 3, unique = true)
    private String tipoSanguineo;

    @Column(name = "quantidade_bolsas", nullable = false)
    private int quantidadeBolsas;

    @Column(name = "capacidade_alvo", nullable = false)
    private int capacidadeAlvo;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}
