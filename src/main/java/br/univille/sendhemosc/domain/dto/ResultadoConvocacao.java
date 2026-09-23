package br.univille.sendhemosc.domain.dto;

import br.univille.sendhemosc.domain.enums.NivelEstoque;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;

/**
 * Resumo de uma rodada de convocacao.
 *
 * @param tipoSanguineo tipo que motivou a convocacao
 * @param nivel nivel do estoque no momento do disparo
 * @param totalElegiveis doadores aptos e dentro dos limites de contato
 * @param totalEnviados quantidade de e-mails efetivamente enviados
 * @param totalFalhas quantidade de envios que falharam
 * @param totalRetidos aptos que ficaram de fora pelos limites de contato: convocados ha pouco
 *                     tempo ou sem resposta a convocacoes demais
 * @param interrompida se a rodada parou antes do fim por falhas seguidas no envio
 */
public record ResultadoConvocacao(
        TipoSanguineo tipoSanguineo,
        NivelEstoque nivel,
        int totalElegiveis,
        int totalEnviados,
        int totalFalhas,
        int totalRetidos,
        boolean interrompida) {
}
