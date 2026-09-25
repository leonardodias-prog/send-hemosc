package br.univille.sendhemosc.adapter.outbound.persistence;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.AuditoriaEntity;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.AuditoriaJpaRepository;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grava o registro de auditoria, obtendo o autor do contexto de seguranca. Acao disparada por
 * rotina agendada fica registrada como sistema, sem usuario.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditoriaPersistenceAdapter implements IAuditoriaPort {

    private final AuditoriaJpaRepository auditoriaRepository;
    private final AutorDaAcao autorDaAcao;

    @Override
    @Transactional
    public void registrar(final String acao, final String detalhe) {
        final String autor = autorDaAcao.atual();

        auditoriaRepository.save(AuditoriaEntity.builder()
                .usuarioEmail(autor)
                .acao(acao)
                .detalhe(detalhe)
                .ocorridoEm(LocalDateTime.now())
                .build());

        log.info("[m=registrar] {} por {}: {}", acao, autor, detalhe);
    }
}
