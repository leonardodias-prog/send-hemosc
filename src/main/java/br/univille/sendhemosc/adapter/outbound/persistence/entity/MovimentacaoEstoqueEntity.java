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
 * Uma alteracao do estoque de um tipo sanguineo: quantidade e capacidade alvo antes e depois,
 * com quem alterou.
 */
@Entity
@Table(name = "movimentacao_estoque")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimentacaoEstoqueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_sanguineo", nullable = false, length = 3)
    private String tipoSanguineo;

    @Column(name = "quantidade_anterior", nullable = false)
    private int quantidadeAnterior;

    @Column(name = "quantidade_nova", nullable = false)
    private int quantidadeNova;

    @Column(name = "capacidade_anterior", nullable = false)
    private int capacidadeAnterior;

    @Column(name = "capacidade_nova", nullable = false)
    private int capacidadeNova;

    @Column(name = "usuario_email", nullable = false, length = 180)
    private String usuarioEmail;

    @Column(name = "ocorrido_em", nullable = false)
    private LocalDateTime ocorridoEm;
}
