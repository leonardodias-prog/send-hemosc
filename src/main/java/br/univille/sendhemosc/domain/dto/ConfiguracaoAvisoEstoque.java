package br.univille.sendhemosc.domain.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Configuracao do aviso interno de estoque. O aviso esta sempre ligado: so a hora do relatorio
 * diario se ajusta.
 *
 * @param horaRelatorio hora cheia, no fuso de Sao Paulo, a partir da qual o relatorio do dia sai
 * @param ultimoRelatorioEm dia do ultimo relatorio enviado, ou nulo se nenhum saiu
 * @param atualizadoPor quem alterou por ultimo, ou nulo na configuracao inicial
 * @param atualizadoEm quando foi alterada por ultimo
 */
public record ConfiguracaoAvisoEstoque(
        int horaRelatorio,
        LocalDate ultimoRelatorioEm,
        String atualizadoPor,
        LocalDateTime atualizadoEm) {
}
