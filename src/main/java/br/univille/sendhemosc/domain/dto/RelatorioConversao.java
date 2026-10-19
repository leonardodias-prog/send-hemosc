package br.univille.sendhemosc.domain.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Relatorio de conversao: quantos convocados viraram doacao, por tipo sanguineo, em um periodo.
 *
 * @param inicio data inicial do periodo, inclusive
 * @param fim data final do periodo, inclusive
 * @param porTipo uma linha por tipo sanguineo, incluindo os sem movimento no periodo
 */
public record RelatorioConversao(LocalDate inicio, LocalDate fim, List<ConversaoPorTipo> porTipo) {

    public long totalConvocados() {
        return porTipo.stream().mapToLong(ConversaoPorTipo::totalConvocados).sum();
    }

    public long totalConvertidos() {
        return porTipo.stream().mapToLong(ConversaoPorTipo::totalConvertidos).sum();
    }

    /**
     * Percentual geral de convocacoes que viraram doacao, somando todos os tipos.
     *
     * @return taxa de conversao, de 0 a 100; zero quando nao houve convocacao no periodo
     */
    public double taxaConversaoGeral() {
        final long total = totalConvocados();
        return total == 0 ? 0.0 : (totalConvertidos() * 100.0) / total;
    }
}
