package br.univille.sendhemosc.adapter.outbound.persistence;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.UsuarioEntity;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.UsuarioJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Descobre quem fez a acao que esta sendo gravada, a partir do contexto de seguranca. Acao
 * disparada por rotina agendada, sem ninguem autenticado, fica registrada como sistema.
 */
@Component
@RequiredArgsConstructor
public class AutorDaAcao {

    static final String AUTOR_SISTEMA = "sistema";

    private final UsuarioJpaRepository usuarioRepository;

    /**
     * Identifica o autor da requisicao em curso.
     *
     * @return e-mail do usuario autenticado, ou "sistema"
     */
    public String atual() {
        final Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();

        if (autenticacao == null || !autenticacao.isAuthenticated()
                || "anonymousUser".equals(autenticacao.getName())) {
            return AUTOR_SISTEMA;
        }

        return autenticacao.getName();
    }

    /**
     * Identificador da conta do autor da requisicao em curso. O e-mail continua sendo gravado
     * ao lado, para o rastro permanecer legivel se a conta for removida.
     *
     * @return o identificador, vazio quando a acao e do sistema ou a conta ja nao existe
     */
    public Optional<Long> idAtual() {
        final String autor = atual();

        if (AUTOR_SISTEMA.equals(autor)) {
            return Optional.empty();
        }

        return usuarioRepository.findByEmailIgnoreCase(autor).map(UsuarioEntity::getId);
    }
}
