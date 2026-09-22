package br.univille.sendhemosc.adapter.outbound.persistence;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.DoacaoEntity;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.DoacaoJpaRepository;
import br.univille.sendhemosc.domain.dto.NovaDoacao;
import br.univille.sendhemosc.domain.port.outbound.IDoacaoRepositoryPort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementacao de persistencia da porta de doacoes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DoacaoPersistenceAdapter implements IDoacaoRepositoryPort {

    private final DoacaoJpaRepository doacaoRepository;

    @Override
    @Transactional
    public Long registrar(final NovaDoacao doacao) {
        final DoacaoEntity entidade = doacaoRepository.save(DoacaoEntity.builder()
                .doadorId(doacao.doadorId())
                .dataDoacao(doacao.dataDoacao())
                .localColeta(doacao.localColeta())
                .observacao(doacao.observacao())
                .criadoEm(LocalDateTime.now())
                .build());

        log.info("[m=registrar] Doacao {} registrada para o doador {} em {}",
                entidade.getId(), doacao.doadorId(), doacao.dataDoacao());

        return entidade.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeNoDia(final Long doadorId, final LocalDate data) {
        return doacaoRepository.existsByDoadorIdAndDataDoacao(doadorId, data);
    }
}
