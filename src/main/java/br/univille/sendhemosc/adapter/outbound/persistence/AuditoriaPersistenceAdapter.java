package br.univille.sendhemosc.adapter.outbound.persistence;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.AuditoriaEntity;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.AuditoriaJpaRepository;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    private static final String AUTOR_SISTEMA = "sistema";

    private final AuditoriaJpaRepository auditoriaRepository;

    @Override
    @Transactional
    public void registrar(final String acao, final String detalhe) {
        final String autor = autorAtual();

        auditoriaRepository.save(AuditoriaEntity.builder()
                .usuarioEmail(autor)
                .acao(acao)
                .detalhe(detalhe)
                .ocorridoEm(LocalDateTime.now())
                .build());

        log.info("[m=registrar] {} por {}: {}", acao, autor, detalhe);
    }

    private String autorAtual() {
        final Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();

        if (autenticacao == null || !autenticacao.isAuthenticated()
                || "anonymousUser".equals(autenticacao.getName())) {
            return AUTOR_SISTEMA;
        }

        return autenticacao.getName();
    }
}
