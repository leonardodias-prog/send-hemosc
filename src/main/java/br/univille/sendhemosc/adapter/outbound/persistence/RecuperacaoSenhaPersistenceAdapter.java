package br.univille.sendhemosc.adapter.outbound.persistence;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.RecuperacaoSenhaEntity;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.RecuperacaoSenhaJpaRepository;
import br.univille.sendhemosc.domain.dto.PedidoDeRecuperacao;
import br.univille.sendhemosc.domain.port.outbound.IRecuperacaoSenhaPort;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementacao de persistencia da porta de recuperacao de senha.
 */
@Component
@RequiredArgsConstructor
public class RecuperacaoSenhaPersistenceAdapter implements IRecuperacaoSenhaPort {

    private final RecuperacaoSenhaJpaRepository recuperacaoRepository;

    @Override
    @Transactional
    public void registrar(final Long usuarioId, final String tokenHash,
                          final LocalDateTime criadoEm, final LocalDateTime expiraEm) {
        recuperacaoRepository.save(RecuperacaoSenhaEntity.builder()
                .usuarioId(usuarioId)
                .tokenHash(tokenHash)
                .criadoEm(criadoEm)
                .expiraEm(expiraEm)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PedidoDeRecuperacao> buscarPorTokenHash(final String tokenHash) {
        return recuperacaoRepository.findByTokenHash(tokenHash)
                .map(entidade -> new PedidoDeRecuperacao(entidade.getId(), entidade.getUsuarioId(),
                        entidade.getExpiraEm(), entidade.getUsadoEm()));
    }

    @Override
    @Transactional
    public boolean usar(final Long id, final LocalDateTime quando) {
        return recuperacaoRepository.usar(id, quando) == 1;
    }

    @Override
    @Transactional
    public void invalidarAbertos(final Long usuarioId, final LocalDateTime quando) {
        recuperacaoRepository.invalidarAbertos(usuarioId, quando);
    }

    @Override
    @Transactional(readOnly = true)
    public long contarDaConta(final Long usuarioId, final LocalDateTime desde) {
        return recuperacaoRepository.countByUsuarioIdAndCriadoEmAfter(usuarioId, desde);
    }

    @Override
    @Transactional(readOnly = true)
    public long contarTodos(final LocalDateTime desde) {
        return recuperacaoRepository.countByCriadoEmAfter(desde);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LocalDateTime> ultimoDaConta(final Long usuarioId) {
        return recuperacaoRepository.ultimoDaConta(usuarioId);
    }

    @Override
    @Transactional
    public void apagarAnterioresA(final LocalDateTime limite) {
        recuperacaoRepository.apagarAnterioresA(limite);
    }
}
