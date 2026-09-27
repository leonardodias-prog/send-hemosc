package br.univille.sendhemosc.config;

import br.univille.sendhemosc.domain.dto.UsuarioAutenticavel;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import br.univille.sendhemosc.usecase.usuario.LimiteDeTentativasDeLogin;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * Desfecho de uma entrada recusada: conta a falha no limite de tentativas, registra na auditoria
 * e devolve a tela de entrada com a mensagem certa.
 *
 * <p>So entra na auditoria a falha contra conta que existe. Tentativa com e-mail sem conta nao
 * tem de quem ser rastro, e grava-la deixaria qualquer um encher o banco digitando e-mails
 * inventados.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FalhaAoEntrar implements AuthenticationFailureHandler {

    private final LimiteDeTentativasDeLogin limiteDeTentativas;
    private final IUsuarioRepositoryPort usuarioRepository;
    private final IAuditoriaPort auditoria;

    @Override
    public void onAuthenticationFailure(final HttpServletRequest requisicao, final HttpServletResponse resposta,
                                        final AuthenticationException excecao) throws IOException {
        final String email = normalizar(requisicao.getParameter("email"));
        final boolean bloqueou = !email.isEmpty() && limiteDeTentativas.registrarFalha(email);

        usuarioRepository.buscarPorEmail(email).ifPresentOrElse(
                conta -> auditar(conta, excecao, bloqueou),
                () -> {
                    if (bloqueou) {
                        log.warn("[m=onAuthenticationFailure] Entrada bloqueada por tentativas seguidas com um e-mail sem conta");
                    }
                });

        resposta.sendRedirect(requisicao.getContextPath() + (bloqueou ? "/login?bloqueado" : "/login?erro"));
    }

    private void auditar(final UsuarioAutenticavel conta, final AuthenticationException excecao,
                         final boolean bloqueou) {
        final String quem = "%s (%s)".formatted(conta.nome(), conta.email());
        final String motivo = excecao instanceof DisabledException
                ? "conta em situacao " + conta.situacao()
                : "senha incorreta";

        auditoria.registrar("ENTRADA_RECUSADA", "%s: %s".formatted(quem, motivo));

        if (bloqueou) {
            auditoria.registrar("ENTRADA_BLOQUEADA", "%s: %d tentativas erradas seguidas, bloqueada por %d minutos"
                    .formatted(quem, limiteDeTentativas.getMaxFalhas(), limiteDeTentativas.getBloqueioMinutos()));
        }
    }

    private static String normalizar(final String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
