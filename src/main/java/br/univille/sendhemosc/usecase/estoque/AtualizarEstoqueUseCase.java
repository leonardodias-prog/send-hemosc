package br.univille.sendhemosc.usecase.estoque;

import br.univille.sendhemosc.domain.dto.MovimentacaoEstoque;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.exception.EstoqueErrorsMessage;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IEstoqueRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.IEstoqueRepositoryPort.RegistroEstoque;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Atualiza a quantidade de bolsas e a capacidade alvo de um tipo sanguineo.
 *
 * <p>A capacidade alvo e a referencia da classificacao em normal, atencao e critico: muda-la muda
 * quem e convocado. Por isso as duas alteracoes ficam no historico de movimentacao, com o antes,
 * o depois e quem alterou.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AtualizarEstoqueUseCase {

    private final IEstoqueRepositoryPort estoqueRepository;

    /**
     * Executa a atualizacao.
     *
     * @param tipoSanguineo tipo a atualizar
     * @param quantidadeBolsas nova quantidade, zero ou mais
     * @param capacidadeAlvo nova capacidade alvo, maior que zero; nula mantem a atual
     * @return movimentacao registrada, ou vazio quando nada mudou
     */
    public Optional<MovimentacaoEstoque> execute(final TipoSanguineo tipoSanguineo, final int quantidadeBolsas,
                                                 final Integer capacidadeAlvo) {
        if (quantidadeBolsas < 0) {
            throw new NegocioException(EstoqueErrorsMessage.QUANTIDADE_NEGATIVA);
        }

        if (capacidadeAlvo != null && capacidadeAlvo <= 0) {
            throw new NegocioException(EstoqueErrorsMessage.CAPACIDADE_INVALIDA);
        }

        final RegistroEstoque atual = estoqueRepository.buscarPorTipo(tipoSanguineo)
                .orElseThrow(() -> new NegocioException(EstoqueErrorsMessage.NAO_ENCONTRADO));
        final int capacidade = capacidadeAlvo != null ? capacidadeAlvo : atual.capacidadeAlvo();

        final Optional<MovimentacaoEstoque> movimentacao =
                estoqueRepository.atualizar(tipoSanguineo, quantidadeBolsas, capacidade);

        if (movimentacao.isEmpty()) {
            log.info("[m=execute] Estoque {} ja estava com {} de {} bolsas", tipoSanguineo.getSigla(),
                    quantidadeBolsas, capacidade);
        }

        return movimentacao;
    }
}
