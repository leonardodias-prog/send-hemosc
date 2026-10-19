package br.univille.sendhemosc.domain.dto;

import br.univille.sendhemosc.domain.enums.TipoSanguineo;

/**
 * Quantas convocacoes enviadas para um tipo sanguineo terminaram em doacao, em um periodo.
 *
 * @param tipoSanguineo tipo sanguineo que motivou as convocacoes
 * @param totalConvocados convocacoes enviadas no periodo (uma por envio, nao por doador)
 * @param totalConvertidos dessas, quantas fecharam em doacao registrada
 */
public record ConversaoPorTipo(TipoSanguineo tipoSanguineo, long totalConvocados, long totalConvertidos) {

    /**
     * Percentual de convocacoes que viraram doacao.
     *
     * @return taxa de conversao, de 0 a 100; zero quando nao houve convocacao no periodo
     */
    public double taxaConversao() {
        return totalConvocados == 0 ? 0.0 : (totalConvertidos * 100.0) / totalConvocados;
    }
}
