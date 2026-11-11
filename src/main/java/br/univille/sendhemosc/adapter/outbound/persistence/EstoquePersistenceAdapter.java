package br.univille.sendhemosc.adapter.outbound.persistence;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.EstoqueEntity;
import br.univille.sendhemosc.adapter.outbound.persistence.entity.MovimentacaoEstoqueEntity;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.EstoqueJpaRepository;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.MovimentacaoEstoqueJpaRepository;
import br.univille.sendhemosc.domain.dto.MovimentacaoEstoque;
import br.univille.sendhemosc.domain.enums.NivelEstoque;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.exception.EstoqueErrorsMessage;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IEstoqueRepositoryPort;
import br.univille.sendhemosc.domain.util.MascaraDeEmail;
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
                quantidadeBolsas, capacidadeAlvo, MascaraDeEmail.mascarar(movimentacao.getUsuarioEmail()));

        return Optional.of(paraMovimentacao(movimentacao));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimentacaoEstoque> listarMovimentacoes(final int limite) {
        return movimentacaoRepository.findAllByOrderByOcorridoEmDescIdDesc(PageRequest.of(0, limite)).stream()
                .map(this::paraMovimentacao)
                .toList();
    }

    @Override
    @Transactional
    public boolean atualizarMinimos(final TipoSanguineo tipoSanguineo, final int minimoCritico, final int minimoBaixo) {
        final EstoqueEntity entidade = entidadeDo(tipoSanguineo);

        if (entidade.getMinimoCritico() == minimoCritico && entidade.getMinimoBaixo() == minimoBaixo) {
            return false;
        }

        entidade.setMinimoCritico(minimoCritico);
        entidade.setMinimoBaixo(minimoBaixo);
        estoqueRepository.save(entidade);

        log.info("[m=atualizarMinimos] Minimos de {} agora: baixo abaixo de {}, critico abaixo de {}",
                tipoSanguineo.getSigla(), minimoBaixo, minimoCritico);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NivelEstoque> nivelAvisado(final TipoSanguineo tipoSanguineo) {
        return estoqueRepository.findByTipoSanguineo(tipoSanguineo.getSigla()).map(EstoqueEntity::getNivelAvisado);
    }

    @Override
    @Transactional
    public void registrarNivelAvisado(final TipoSanguineo tipoSanguineo, final NivelEstoque nivel) {
        final EstoqueEntity entidade = entidadeDo(tipoSanguineo);

        // Normal nao se guarda: coluna vazia e o tipo rearmado para o proximo alerta.
        entidade.setNivelAvisado(nivel == NivelEstoque.NORMAL ? null : nivel);
        estoqueRepository.save(entidade);
    }

    private EstoqueEntity entidadeDo(final TipoSanguineo tipoSanguineo) {
        return estoqueRepository.findByTipoSanguineo(tipoSanguineo.getSigla())
                .orElseThrow(() -> new NegocioException(EstoqueErrorsMessage.NAO_ENCONTRADO));
    }

    private RegistroEstoque paraRegistro(final EstoqueEntity entidade) {
        return new RegistroEstoque(
                TipoSanguineo.doSigla(entidade.getTipoSanguineo()),
                entidade.getQuantidadeBolsas(),
                entidade.getCapacidadeAlvo(),
                entidade.getMinimoCritico(),
                entidade.getMinimoBaixo());
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
