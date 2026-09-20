package br.univille.sendhemosc.adapter.outbound.persistence.entity;

import br.univille.sendhemosc.domain.enums.NivelEstoque;
import br.univille.sendhemosc.domain.enums.OrigemNotificacao;
import br.univille.sendhemosc.domain.enums.StatusNotificacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Registro de uma convocacao enviada ao doador. Sustenta a regra de reenvio mensal.
 */
@Entity
@Table(name = "notificacao")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacaoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doador_id", nullable = false)
    private Long doadorId;

    @Column(name = "tipo_sanguineo_alvo", nullable = false, length = 3)
    private String tipoSanguineoAlvo;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_estoque", nullable = false, length = 20)
    private NivelEstoque nivelEstoque;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusNotificacao status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigemNotificacao origem;

    @Column(nullable = false)
    private int tentativa;

    @Column(name = "enviada_em")
    private LocalDateTime enviadaEm;

    @Column(name = "compareceu_em")
    private LocalDate compareceuEm;

    @Column(name = "erro_mensagem", length = 500)
    private String erroMensagem;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}
