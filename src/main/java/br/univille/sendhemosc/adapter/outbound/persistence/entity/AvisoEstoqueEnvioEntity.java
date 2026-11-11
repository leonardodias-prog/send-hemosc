package br.univille.sendhemosc.adapter.outbound.persistence.entity;

import br.univille.sendhemosc.domain.enums.TipoAvisoEstoque;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Registro de um aviso interno de estoque enviado.
 */
@Entity
@Table(name = "aviso_estoque_envio")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvisoEstoqueEnvioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoAvisoEstoque tipo;

    @Column(nullable = false, length = 200)
    private String assunto;

    @Column(nullable = false)
    private int destinatarios;

    @Column(nullable = false)
    private int falhas;

    @Column(name = "enviado_em", nullable = false)
    private LocalDateTime enviadoEm;
}
