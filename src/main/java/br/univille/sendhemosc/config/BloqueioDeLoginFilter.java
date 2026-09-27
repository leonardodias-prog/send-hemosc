package br.univille.sendhemosc.config;

import br.univille.sendhemosc.usecase.usuario.LimiteDeTentativasDeLogin;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Recusa a entrada de e-mail bloqueado antes de conferir a senha: durante o bloqueio, nem a
 * senha certa entra. E o que impede testar senhas em sequencia.
 *
 * <p>Nao e um bean de proposito. Como bean, o Spring Boot o registraria tambem como filtro comum
 * do servidor, e ele rodaria duas vezes, uma delas fora da cadeia de seguranca.</p>
 */
public class BloqueioDeLoginFilter extends OncePerRequestFilter {

    private final LimiteDeTentativasDeLogin limiteDeTentativas;

    public BloqueioDeLoginFilter(final LimiteDeTentativasDeLogin limiteDeTentativas) {
        this.limiteDeTentativas = limiteDeTentativas;
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest requisicao) {
        final String caminho = requisicao.getRequestURI().substring(requisicao.getContextPath().length());

        return !("POST".equals(requisicao.getMethod()) && "/login".equals(caminho));
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest requisicao, final HttpServletResponse resposta,
                                    final FilterChain cadeia) throws ServletException, IOException {
        if (limiteDeTentativas.bloqueado(requisicao.getParameter("email"))) {
            resposta.sendRedirect(requisicao.getContextPath() + "/login?bloqueado");
            return;
        }

        cadeia.doFilter(requisicao, resposta);
    }
}
