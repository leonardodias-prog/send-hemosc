package br.univille.sendhemosc.adapter.outbound.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Configuracao do disparo automatico. Existe uma unica linha, criada pela migration.
 */
@Entity
@Table(name = "disparo_automatico")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisparoAutomaticoEntity {

    @Id
    private Long id;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "tipos_sanguineos", nullable = false, length = 40)
    private String tiposSanguineos;

    @Column(name = "limite_por_rodada", nullable = false)
    private int limitePorRodada;

    @Column(name = "hora_inicio", nullable = false)
    private int horaInicio;

    @Column(name = "hora_fim", nullable = false)
    private int horaFim;

    @Column(name = "intervalo_dias", nullable = false)
    private int intervaloDias;

    @Column(name = "ultima_rodada_em")
    private LocalDateTime ultimaRodadaEm;

    @Column(name = "ultima_rodada_resumo", length = 300)
    private String ultimaRodadaResumo;

    @Column(name = "atualizado_por", length = 180)
    private String atualizadoPor;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;
}
