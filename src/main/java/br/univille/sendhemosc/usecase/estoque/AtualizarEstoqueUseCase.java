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
 * <p>As duas alteracoes ficam no historico de movimentacao, com o antes, o depois e quem
 * alterou.</p>
 *
 * <p>E aqui que a equipe fica sabendo que um tipo caiu de nivel: depois de gravar, o aviso
 * interno confere se ha alerta a mandar. Falha no aviso nao desfaz nem impede a atualizacao;
 * registrar a bolsa vem antes de avisar sobre ela.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AtualizarEstoqueUseCase {

    private final IEstoqueRepositoryPort estoqueRepository;
    private final AvisarEquipeDoEstoqueUseCase avisarEquipe;

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

        if (movimentacao.isPresent()) {
            alertarEquipe(tipoSanguineo);
        }

        return movimentacao;
    }

    private void alertarEquipe(final TipoSanguineo tipoSanguineo) {
        try {
            avisarEquipe.avaliarAlerta(tipoSanguineo);
        } catch (final RuntimeException excecao) {
            log.error("[m=alertarEquipe] Estoque de {} atualizado, mas o alerta a equipe falhou",
                    tipoSanguineo.getSigla(), excecao);
        }
    }
}
