package br.univille.sendhemosc.usecase.estoque;

import br.univille.sendhemosc.config.SendHemoscProperties;
import br.univille.sendhemosc.domain.dto.SituacaoEstoque;
import br.univille.sendhemosc.domain.enums.NivelEstoque;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.exception.EstoqueErrorsMessage;
import br.univille.sendhemosc.domain.exception.NegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Classifica o estoque de um tipo sanguineo em NORMAL, ATENCAO ou CRITICO, comparando a quantidade
 * atual com a capacidade alvo. Os limiares percentuais vem de application.yml.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassificarNivelEstoqueUseCase {

    private final SendHemoscProperties properties;

    /**
     * Classifica o estoque informado.
     *
     * @param tipoSanguineo tipo avaliado
     * @param quantidadeBolsas quantidade atual em estoque
     * @param capacidadeAlvo capacidade considerada ideal
     * @return situacao consolidada, com percentual de ocupacao e nivel
     */
    public SituacaoEstoque execute(final TipoSanguineo tipoSanguineo, final int quantidadeBolsas, final int capacidadeAlvo) {
        if (quantidadeBolsas < 0) {
            throw new NegocioException(EstoqueErrorsMessage.QUANTIDADE_NEGATIVA);
        }
        if (capacidadeAlvo <= 0) {
            throw new NegocioException(EstoqueErrorsMessage.CAPACIDADE_INVALIDA);
        }

        final SendHemoscProperties.Estoque limiares = properties.estoque();
        final int percentual = (int) Math.floor((double) quantidadeBolsas * 100 / capacidadeAlvo);

        final NivelEstoque nivel;
        if (percentual < limiares.limiarCriticoPercentual()) {
            nivel = NivelEstoque.CRITICO;
        } else if (percentual < limiares.limiarAtencaoPercentual()) {
            nivel = NivelEstoque.ATENCAO;
        } else {
            nivel = NivelEstoque.NORMAL;
        }

        return new SituacaoEstoque(tipoSanguineo, quantidadeBolsas, capacidadeAlvo, percentual, nivel);
    }
}
