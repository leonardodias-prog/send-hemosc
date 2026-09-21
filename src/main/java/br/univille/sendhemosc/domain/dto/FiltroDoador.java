package br.univille.sendhemosc.domain.dto;

import br.univille.sendhemosc.domain.enums.TipoSanguineo;

/**
 * Criterios de busca na listagem de doadores.
 *
 * @param busca trecho de nome ou e-mail, nulo para nao filtrar
 * @param tipoSanguineo tipo desejado, nulo para todos
 * @param apenasAptos quando true, descarta quem nao pode doar na data de hoje
 * @param apenasComConsentimento quando true, descarta quem nao autorizou contato
 */
public record FiltroDoador(
        String busca,
        TipoSanguineo tipoSanguineo,
        boolean apenasAptos,
        boolean apenasComConsentimento) {

    /**
     * Filtro sem restricao alguma.
     *
     * @return filtro vazio
     */
    public static FiltroDoador vazio() {
        return new FiltroDoador(null, null, false, false);
    }

    public boolean temBusca() {
        return busca != null && !busca.isBlank();
    }

    /**
     * Trecho de busca pronto para comparacao, em minusculas e entre curingas.
     *
     * @return padrao para a clausula de comparacao textual
     */
    public String buscaComoPadrao() {
        return temBusca() ? "%" + busca.trim().toLowerCase() + "%" : "%";
    }
}
