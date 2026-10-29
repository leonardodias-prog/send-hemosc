package br.univille.sendhemosc.adapter.inbound.http.controller;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * Monta o endereco que os links de pagina completam com o numero. Leva junto o filtro em uso,
 * para que trocar de pagina nao descarte o recorte.
 */
final class ParaPaginacao {

    private ParaPaginacao() {
    }

    /**
     * @param endereco caminho da tela com os parametros do filtro em uso
     * @return endereco terminado em "pagina=", pronto para receber o numero
     */
    static String url(final UriComponentsBuilder endereco) {
        final String base = endereco.build().encode().toUriString();
        return base + (base.contains("?") ? "&" : "?") + "pagina=";
    }
}
