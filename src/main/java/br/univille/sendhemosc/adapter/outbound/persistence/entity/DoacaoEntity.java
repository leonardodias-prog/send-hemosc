package br.univille.sendhemosc.adapter.outbound.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
 * Doacao realizada por um doador. E a base do calculo de intervalo entre doacoes.
 */
@Entity
@Table(name = "doacao")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoacaoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doador_id", nullable = false)
    private Long doadorId;

    @Column(name = "data_doacao", nullable = false)
    private LocalDate dataDoacao;

    @Column(name = "local_coleta", length = 150)
    private String localColeta;

    @Column(length = 500)
    private String observacao;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}
