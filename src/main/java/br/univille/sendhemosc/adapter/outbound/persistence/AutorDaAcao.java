package br.univille.sendhemosc.adapter.outbound.persistence;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Descobre quem fez a acao que esta sendo gravada, a partir do contexto de seguranca. Acao
 * disparada por rotina agendada, sem ninguem autenticado, fica registrada como sistema.
 */
@Component
public class AutorDaAcao {

    static final String AUTOR_SISTEMA = "sistema";

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
}
