package br.univille.sendhemosc.domain.port.outbound;

import br.univille.sendhemosc.domain.dto.Pagina;
import br.univille.sendhemosc.domain.dto.RegistroAuditoria;
import java.util.List;

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

    /**
     * Lista os registros, do mais novo para o mais antigo.
     *
     * @param acao somente esta acao; nulo ou vazio traz todas
     * @param pagina trecho pedido, a partir de 1
     * @param tamanho registros por trecho
     * @return o trecho pedido
     */
    Pagina<RegistroAuditoria> listar(String acao, int pagina, int tamanho);

    /**
     * Acoes que ja aparecem no registro, para o filtro da tela.
     *
     * @return identificadores em ordem alfabetica
     */
    List<String> acoesRegistradas();
}
