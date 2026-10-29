package br.univille.sendhemosc.domain.dto;

import java.util.List;

/**
 * Um trecho de uma listagem longa, com o que a tela precisa para navegar entre os trechos.
 *
 * @param itens itens deste trecho
 * @param numero numero do trecho, a partir de 1
 * @param tamanho quantidade maxima de itens por trecho
 * @param total quantidade de itens da listagem inteira
 * @param <T> tipo do item
 */
public record Pagina<T>(List<T> itens, int numero, int tamanho, long total) {

    /**
     * Recorta uma lista ja carregada. Numero fora do intervalo cai no trecho valido mais
     * proximo: um link antigo para a pagina 9 de uma lista que encolheu mostra a ultima.
     *
     * @param todos lista completa
     * @param numero trecho pedido, a partir de 1
     * @param tamanho itens por trecho
     * @param <T> tipo do item
     * @return o trecho pedido
     */
    public static <T> Pagina<T> recortar(final List<T> todos, final int numero, final int tamanho) {
        final int ultima = Math.max(1, (int) Math.ceil(todos.size() / (double) tamanho));
        final int valido = Math.min(Math.max(1, numero), ultima);
        final int inicio = (valido - 1) * tamanho;

        return new Pagina<>(todos.subList(inicio, Math.min(inicio + tamanho, todos.size())),
                valido, tamanho, todos.size());
    }

    public int totalPaginas() {
        return Math.max(1, (int) Math.ceil(total / (double) tamanho));
    }

    public boolean temAnterior() {
        return numero > 1;
    }

    public boolean temProxima() {
        return numero < totalPaginas();
    }

    /** Posicao do primeiro item deste trecho na listagem inteira, a partir de 1. */
    public long primeiroItem() {
        return itens.isEmpty() ? 0 : (long) (numero - 1) * tamanho + 1;
    }

    /** Posicao do ultimo item deste trecho na listagem inteira. */
    public long ultimoItem() {
        return itens.isEmpty() ? 0 : primeiroItem() + itens.size() - 1;
    }
}
