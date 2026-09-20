package br.univille.sendhemosc.usecase.estoque;

import br.univille.sendhemosc.domain.dto.SituacaoEstoque;
import br.univille.sendhemosc.domain.port.outbound.IEstoqueRepositoryPort;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Lista a situacao de todos os tipos sanguineos, ja classificada por nivel e ordenada
 * do mais critico para o mais folgado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ListarSituacaoEstoqueUseCase {

    private final IEstoqueRepositoryPort estoqueRepository;
    private final ClassificarNivelEstoqueUseCase classificarNivel;

    /**
     * Executa a listagem.
     *
     * @return situacao de cada tipo sanguineo, ordenada por criticidade
     */
    public List<SituacaoEstoque> execute() {
        final List<SituacaoEstoque> situacoes = estoqueRepository.listarTodos().stream()
                .map(registro -> classificarNivel.execute(
                        registro.tipoSanguineo(), registro.quantidadeBolsas(), registro.capacidadeAlvo()))
                .sorted(Comparator.comparingInt(SituacaoEstoque::percentualOcupacao))
                .toList();

        log.info("[m=execute] Situacao de estoque consolidada para {} tipos sanguineos", situacoes.size());
        return situacoes;
    }
}
