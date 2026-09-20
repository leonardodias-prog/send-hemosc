package br.univille.sendhemosc.domain.dto;

import br.univille.sendhemosc.domain.enums.NivelEstoque;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;

/**
 * Situacao consolidada do estoque de um tipo sanguineo.
 *
 * @param tipoSanguineo tipo avaliado
 * @param quantidadeBolsas quantidade atual em estoque
 * @param capacidadeAlvo quantidade considerada ideal
 * @param percentualOcupacao percentual da capacidade alvo atualmente ocupado
 * @param nivel classificacao resultante
 */
public record SituacaoEstoque(
        TipoSanguineo tipoSanguineo,
        int quantidadeBolsas,
        int capacidadeAlvo,
        int percentualOcupacao,
        NivelEstoque nivel) {
}
