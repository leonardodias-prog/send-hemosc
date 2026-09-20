package br.univille.sendhemosc.adapter.outbound.persistence;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.EstoqueEntity;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.EstoqueJpaRepository;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.exception.EstoqueErrorsMessage;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IEstoqueRepositoryPort;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    @Transactional
    public void atualizarQuantidade(final TipoSanguineo tipoSanguineo, final int quantidadeBolsas) {
        final EstoqueEntity entidade = estoqueRepository.findByTipoSanguineo(tipoSanguineo.getSigla())
                .orElseThrow(() -> new NegocioException(EstoqueErrorsMessage.NAO_ENCONTRADO));

        entidade.setQuantidadeBolsas(quantidadeBolsas);
        entidade.setAtualizadoEm(LocalDateTime.now());
        estoqueRepository.save(entidade);

        log.info("[m=atualizarQuantidade] Estoque {} atualizado para {} bolsas", tipoSanguineo.getSigla(), quantidadeBolsas);
    }

    private RegistroEstoque paraRegistro(final EstoqueEntity entidade) {
        return new RegistroEstoque(
                TipoSanguineo.doSigla(entidade.getTipoSanguineo()),
                entidade.getQuantidadeBolsas(),
                entidade.getCapacidadeAlvo());
    }
}
