package br.univille.sendhemosc.domain.dto;

import java.util.List;

/**
 * Quem seria convocado numa rodada automatica, antes de qualquer envio.
 *
 * <p>A rodada envia exatamente o que o plano diz, e a tela mostra o mesmo plano. E o que permite
 * ver o efeito de ligar o disparo antes de liga-lo.</p>
 *
 * @param itens um por tipo em falta entre os escolhidos, do mais critico ao menos critico
 * @param totalDestinatarios pessoas que recebem nesta rodada, cada uma uma vez so
 * @param totalRetidos pessoas aptas que os limites de contato deixaram de fora
 * @param totalAdiados pessoas que poderiam receber, mas ficaram para a proxima pelo limite da rodada
 */
public record PlanoDeRodada(List<Item> itens, int totalDestinatarios, int totalRetidos, int totalAdiados) {

    /**
     * Parte do plano referente a um tipo sanguineo.
     *
     * @param selecao quem pode ser convocado para o tipo
     * @param destinatarios quem de fato recebe nesta rodada
     */
    public record Item(SelecaoConvocacao selecao, List<CandidatoConvocacao> destinatarios) {
    }
}
