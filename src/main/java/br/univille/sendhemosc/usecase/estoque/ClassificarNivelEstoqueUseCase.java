package br.univille.sendhemosc.usecase.estoque;

import br.univille.sendhemosc.domain.dto.SituacaoEstoque;
import br.univille.sendhemosc.domain.enums.NivelEstoque;
import br.univille.sendhemosc.domain.exception.EstoqueErrorsMessage;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IEstoqueRepositoryPort.RegistroEstoque;
import org.springframework.stereotype.Service;

/**
 * Classifica o estoque de um tipo sanguineo em NORMAL, ATENCAO (baixo) ou CRITICO, comparando a
 * quantidade atual com os dois minimos do proprio tipo, em bolsas.
 *
 * <p>E a regua unica do sistema: painel, convocacao de doadores e aviso interno passam por aqui.
 * A capacidade alvo continua existindo para o percentual de ocupacao que o painel desenha, mas
 * nao decide mais o nivel.</p>
 */
@Service
public class ClassificarNivelEstoqueUseCase {

    private static final int CEM_POR_CENTO = 100;

    /**
     * Classifica o estoque informado.
     *
     * @param registro quantidade, capacidade alvo e minimos do tipo
     * @return situacao consolidada do estoque
     */
    public SituacaoEstoque execute(final RegistroEstoque registro) {
        if (registro.quantidadeBolsas() < 0) {
            throw new NegocioException(EstoqueErrorsMessage.QUANTIDADE_NEGATIVA);
        }
        if (registro.capacidadeAlvo() <= 0) {
            throw new NegocioException(EstoqueErrorsMessage.CAPACIDADE_INVALIDA);
        }

        final int percentual = (int) Math.floor(
                (double) registro.quantidadeBolsas() * CEM_POR_CENTO / registro.capacidadeAlvo());

        final NivelEstoque nivel;
        if (registro.quantidadeBolsas() < registro.minimoCritico()) {
            nivel = NivelEstoque.CRITICO;
        } else if (registro.quantidadeBolsas() < registro.minimoBaixo()) {
            nivel = NivelEstoque.ATENCAO;
        } else {
            nivel = NivelEstoque.NORMAL;
        }

        return new SituacaoEstoque(registro.tipoSanguineo(), registro.quantidadeBolsas(), registro.capacidadeAlvo(),
                percentual, nivel);
    }
}
