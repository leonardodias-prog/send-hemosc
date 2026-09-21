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
 * Prova do consentimento do doador. Guardar apenas um campo booleano no cadastro nao sustenta
 * a exigencia da LGPD: e preciso saber quando o consentimento foi dado e sob qual versao do
 * termo, e manter o historico quando ele for revogado.
 */
@Entity
@Table(name = "consentimento")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentimentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doador_id", nullable = false)
    private Long doadorId;

    @Column(name = "versao_termo", nullable = false, length = 20)
    private String versaoTermo;

    @Column(nullable = false)
    private boolean aceito;

    @Column(name = "registrado_em", nullable = false)
    private LocalDateTime registradoEm;

    @Column(nullable = false, length = 30)
    private String origem;
}
