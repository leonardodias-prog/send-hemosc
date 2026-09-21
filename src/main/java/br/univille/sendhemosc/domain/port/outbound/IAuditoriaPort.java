package br.univille.sendhemosc.domain.port.outbound;

/**
 * Porta de saida para o registro de auditoria. Toda acao que produz efeito fora do sistema
 * passa por aqui, de modo que exista rastro de quem fez o que.
 */
public interface IAuditoriaPort {

    /**
     * Registra uma acao.
     *
     * @param acao identificador curto da acao, em maiusculas
     * @param detalhe descricao do que foi feito
     */
    void registrar(String acao, String detalhe);
}
