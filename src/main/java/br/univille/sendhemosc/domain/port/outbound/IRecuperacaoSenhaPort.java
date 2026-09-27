package br.univille.sendhemosc.domain.port.outbound;

import br.univille.sendhemosc.domain.dto.PedidoDeRecuperacao;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Porta de saida para os pedidos de recuperacao de senha. O token do link nunca chega aqui:
 * so o hash dele, para que quem le o banco nao consiga montar o link.
 */
public interface IRecuperacaoSenhaPort {

    /**
     * Grava um pedido novo.
     *
     * @param usuarioId conta que pediu
     * @param tokenHash hash do token enviado no link
     * @param criadoEm momento do pedido
     * @param expiraEm fim da validade do link
     */
    void registrar(Long usuarioId, String tokenHash, LocalDateTime criadoEm, LocalDateTime expiraEm);

    Optional<PedidoDeRecuperacao> buscarPorTokenHash(String tokenHash);

    /**
     * Fecha o pedido, se ainda estiver aberto. A atualizacao e condicional: o mesmo link nao vale
     * duas vezes, nem em duas abas ao mesmo tempo.
     *
     * @param id pedido a fechar
     * @param quando momento do uso
     * @return true se esta chamada fechou o pedido
     */
    boolean usar(Long id, LocalDateTime quando);

    /**
     * Fecha todo pedido aberto da conta. Vale ao emitir um link novo, que substitui o anterior, e
     * quando a senha muda por qualquer caminho.
     *
     * @param usuarioId conta
     * @param quando momento do fechamento
     */
    void invalidarAbertos(Long usuarioId, LocalDateTime quando);

    long contarDaConta(Long usuarioId, LocalDateTime desde);

    long contarTodos(LocalDateTime desde);

    Optional<LocalDateTime> ultimoDaConta(Long usuarioId);

    /**
     * Remove pedidos antigos, que ja nao contam para nenhum limite.
     *
     * @param limite pedidos criados antes deste momento saem
     */
    void apagarAnterioresA(LocalDateTime limite);
}
