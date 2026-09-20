package br.univille.sendhemosc.domain.dto;

import br.univille.sendhemosc.domain.enums.NivelEstoque;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;

/**
 * Resumo de uma rodada de convocacao.
 *
 * @param tipoSanguineo tipo que motivou a convocacao
 * @param nivel nivel do estoque no momento do disparo
 * @param totalElegiveis quantidade de doadores aptos encontrados
 * @param totalEnviados quantidade de e-mails efetivamente enviados
 * @param totalFalhas quantidade de envios que falharam
 */
public record ResultadoConvocacao(
        TipoSanguineo tipoSanguineo,
        NivelEstoque nivel,
        int totalElegiveis,
        int totalEnviados,
        int totalFalhas) {
}
