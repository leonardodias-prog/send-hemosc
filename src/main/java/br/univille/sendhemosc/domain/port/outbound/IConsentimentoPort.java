package br.univille.sendhemosc.domain.port.outbound;

/**
 * Porta de saida para a prova do consentimento do doador.
 *
 * <p>Um campo booleano no cadastro diz apenas o estado atual. A LGPD exige demonstrar o
 * consentimento, o que pede saber quando ele foi dado, sob qual versao do termo, e manter o
 * historico quando for revogado.</p>
 */
public interface IConsentimentoPort {

    /**
     * Registra uma manifestacao de consentimento.
     *
     * @param doadorId doador que se manifestou
     * @param aceito true quando autorizou o contato, false quando revogou
     * @param origem de onde partiu, como CADASTRO ou LINK_DESCADASTRO
     */
    void registrar(Long doadorId, boolean aceito, String origem);
}
