package br.univille.sendhemosc.domain.dto;

import java.time.LocalDateTime;

/**
 * Pedido de recuperacao de senha, localizado pelo hash do token que vai no link.
 *
 * @param id identificador do pedido
 * @param usuarioId conta a que o pedido pertence
 * @param expiraEm fim da validade do link
 * @param usadoEm quando o link foi usado ou invalidado; nulo enquanto aberto
 */
public record PedidoDeRecuperacao(Long id, Long usuarioId, LocalDateTime expiraEm, LocalDateTime usadoEm) {

    /**
     * Indica se o link ainda serve.
     *
     * @param agora momento da avaliacao
     * @return true quando nao foi usado nem venceu
     */
    public boolean aberto(final LocalDateTime agora) {
        return usadoEm == null && expiraEm.isAfter(agora);
    }
}
