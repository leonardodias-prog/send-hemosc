package br.univille.sendhemosc.adapter.outbound.persistence;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.EstoqueEntity;
import br.univille.sendhemosc.adapter.outbound.persistence.entity.MovimentacaoEstoqueEntity;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.EstoqueJpaRepository;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.MovimentacaoEstoqueJpaRepository;
import br.univille.sendhemosc.domain.dto.MovimentacaoEstoque;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.exception.EstoqueErrorsMessage;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IEstoqueRepositoryPort;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementacao de persistencia da porta de estoque.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EstoquePersistenceAdapter implements IEstoqueRepositoryPort {

    private final EstoqueJpaRepository estoqueRepository;
    private final MovimentacaoEstoqueJpaRepository movimentacaoRepository;
    private final AutorDaAcao autorDaAcao;

    @Override
    @Transactional(readOnly = true)
    public List<RegistroEstoque> listarTodos() {
        return estoqueRepository.findAll().stream()
                .map(this::paraRegistro)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RegistroEstoque> buscarPorTipo(final TipoSanguineo tipoSanguineo) {
        return estoqueRepository.findByTipoSanguineo(tipoSanguineo.getSigla()).map(this::paraRegistro);
    }

    /**
     * O antes vem da mesma leitura que e alterada, dentro da transacao: assim a movimentacao
     * gravada corresponde exatamente ao que foi sobrescrito.
     */
    @Override
    @Transactional
    public Optional<MovimentacaoEstoque> atualizar(final TipoSanguineo tipoSanguineo, final int quantidadeBolsas,
                                                   final int capacidadeAlvo) {
        final EstoqueEntity entidade = estoqueRepository.findByTipoSanguineo(tipoSanguineo.getSigla())
                .orElseThrow(() -> new NegocioException(EstoqueErrorsMessage.NAO_ENCONTRADO));

        if (entidade.getQuantidadeBolsas() == quantidadeBolsas && entidade.getCapacidadeAlvo() == capacidadeAlvo) {
            return Optional.empty();
        }

        final LocalDateTime agora = LocalDateTime.now();
        final MovimentacaoEstoqueEntity movimentacao = movimentacaoRepository.save(MovimentacaoEstoqueEntity.builder()
                .tipoSanguineo(tipoSanguineo.getSigla())
                .quantidadeAnterior(entidade.getQuantidadeBolsas())
                .quantidadeNova(quantidadeBolsas)
                .capacidadeAnterior(entidade.getCapacidadeAlvo())
                .capacidadeNova(capacidadeAlvo)
                .usuarioEmail(autorDaAcao.atual())
                .ocorridoEm(agora)
                .build());

        entidade.setQuantidadeBolsas(quantidadeBolsas);
        entidade.setCapacidadeAlvo(capacidadeAlvo);
        entidade.setAtualizadoEm(agora);
        estoqueRepository.save(entidade);

        log.info("[m=atualizar] Estoque {} atualizado para {} de {} bolsas por {}", tipoSanguineo.getSigla(),
                quantidadeBolsas, capacidadeAlvo, movimentacao.getUsuarioEmail());

        return Optional.of(paraMovimentacao(movimentacao));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimentacaoEstoque> listarMovimentacoes(final int limite) {
        return movimentacaoRepository.findAllByOrderByOcorridoEmDescIdDesc(PageRequest.of(0, limite)).stream()
                .map(this::paraMovimentacao)
                .toList();
    }

    private RegistroEstoque paraRegistro(final EstoqueEntity entidade) {
        return new RegistroEstoque(
                TipoSanguineo.doSigla(entidade.getTipoSanguineo()),
                entidade.getQuantidadeBolsas(),
                entidade.getCapacidadeAlvo());
    }

    private MovimentacaoEstoque paraMovimentacao(final MovimentacaoEstoqueEntity entidade) {
        return new MovimentacaoEstoque(
                TipoSanguineo.doSigla(entidade.getTipoSanguineo()),
                entidade.getQuantidadeAnterior(),
                entidade.getQuantidadeNova(),
                entidade.getCapacidadeAnterior(),
                entidade.getCapacidadeNova(),
                entidade.getUsuarioEmail(),
                entidade.getOcorridoEm());
    }
}
