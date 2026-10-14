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

    /**
     * Troca o nome do doador pelo identificador nos registros sobre doadores. Hoje eles ja nao
     * guardam o nome, mas os gravados antes disso guardam, e a exclusao a pedido do titular nao
     * pode deixar o nome para tras.
     *
     * @param doadorId doador excluido
     * @param nome nome que o doador tinha no cadastro
     */
    void anonimizarDoador(Long doadorId, String nome);
}
