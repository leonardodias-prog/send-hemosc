package br.univille.sendhemosc.domain.dto;

import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Criterios de busca na listagem de doadores.
 *
 * @param busca trecho de nome ou e-mail, nulo para nao filtrar
 * @param tipoSanguineo tipo desejado, nulo para todos
 * @param apenasAptos quando true, descarta quem nao pode doar na data de hoje
 * @param apenasComConsentimento quando true, descarta quem nao autorizou contato
 * @param porCompatibilidade quando true, o tipo desejado busca quem pode doar para ele (tabela
 *         de compatibilidade transfusional), e nao so quem tem exatamente esse tipo. E o mesmo
 *         criterio da convocacao segmentada por tipo.
 */
public record FiltroDoador(
        String busca,
        TipoSanguineo tipoSanguineo,
        boolean apenasAptos,
        boolean apenasComConsentimento,
        boolean porCompatibilidade) {

    /**
     * Filtro sem restricao alguma.
     *
     * @return filtro vazio
     */
    public static FiltroDoador vazio() {
        return new FiltroDoador(null, null, false, false, false);
    }

    public boolean temBusca() {
        return busca != null && !busca.isBlank();
    }

    /**
     * Siglas de tipo sanguineo que a consulta deve aceitar. Sem tipo escolhido, todas; com
     * compatibilidade, o conjunto que pode doar para o tipo escolhido; caso contrario, so o
     * tipo exato.
     *
     * @return siglas aceitas pela busca
     */
    public Set<String> siglasAceitas() {
        if (tipoSanguineo == null) {
            return Arrays.stream(TipoSanguineo.values())
                    .map(TipoSanguineo::getSigla)
                    .collect(Collectors.toSet());
        }

        return porCompatibilidade ? tipoSanguineo.siglasDoadoresCompativeis() : Set.of(tipoSanguineo.getSigla());
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
