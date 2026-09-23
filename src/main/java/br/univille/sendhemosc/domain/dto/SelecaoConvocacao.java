package br.univille.sendhemosc.domain.dto;

import java.util.List;

/**
 * Quem pode ser convocado para um tipo sanguineo, antes de qualquer envio.
 *
 * <p>Separar a selecao do envio e o que permite mostrar na tela quem seria convocado, com a
 * garantia de que o disparo usa exatamente o mesmo criterio.</p>
 *
 * @param situacao situacao do estoque do tipo em falta
 * @param elegiveis compativeis, aptos e dentro dos limites de contato, em ordem de prioridade
 * @param retidos aptos que os limites de contato deixaram de fora
 */
public record SelecaoConvocacao(
        SituacaoEstoque situacao,
        List<CandidatoConvocacao> elegiveis,
        List<CandidatoConvocacao> retidos) {
}
