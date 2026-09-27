package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.usecase.usuario.TrocarPropriaSenhaUseCase;
import java.security.Principal;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * A propria conta de quem esta autenticado. Por ora, a troca de senha, aberta a todo perfil.
 *
 * <p>Senha digitada nunca volta para o formulario, nem depois de erro: devolve-la deixaria o
 * valor no HTML da pagina.</p>
 */
@Controller
@RequiredArgsConstructor
public class ContaViewAdapter {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final TrocarPropriaSenhaUseCase trocarPropriaSenha;
    private final MessageSource messageSource;

    @GetMapping("/conta/senha")
    public String formularioDeSenha() {
        return "conta-senha";
    }

    @PostMapping("/conta/senha")
    public String trocarSenha(@RequestParam(required = false) final String senhaAtual,
                              @RequestParam(required = false) final String novaSenha,
                              @RequestParam(required = false) final String confirmacao,
                              final Principal autenticado,
                              final RedirectAttributes atributos) {
        try {
            trocarPropriaSenha.execute(autenticado.getName(), senhaAtual, novaSenha, confirmacao);
            atributos.addFlashAttribute("aviso", "Senha alterada. Na próxima entrada, use a nova.");
        } catch (final NegocioException excecao) {
            atributos.addFlashAttribute("erro", messageSource.getMessage(
                    excecao.getErro().getChaveMensagem(), null,
                    excecao.getErro().getChaveMensagem(), PT_BR));
        }

        return "redirect:/conta/senha";
    }
}
