package br.univille.sendhemosc.adapter.outbound.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Doador cadastrado. Todos os registros deste prototipo sao ficticios.
 */
@Entity
@Table(name = "doador")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoadorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 180, unique = true)
    private String email;

    @Column(length = 20)
    private String telefone;

    @Column(name = "tipo_sanguineo", nullable = false, length = 3)
    private String tipoSanguineo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private br.univille.sendhemosc.domain.enums.Sexo sexo;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Column(name = "peso_kg", precision = 5, scale = 2)
    private BigDecimal pesoKg;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "aceita_contato", nullable = false)
    private boolean aceitaContato;

    @Column(name = "token_descadastro", nullable = false, length = 64, unique = true)
    private String tokenDescadastro;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}
