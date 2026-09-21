package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.domain.dto.NovoUsuario;
import br.univille.sendhemosc.domain.enums.PerfilUsuario;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.usecase.usuario.CriarUsuarioUseCase;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Telas publicas de acesso: entrar, cadastrar-se e ler o termo de uso.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AutenticacaoViewAdapter {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final CriarUsuarioUseCase criarUsuario;
    private final MessageSource messageSource;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/termo")
    public String termo() {
        return "termo";
    }

    @GetMapping("/cadastro")
    public String formularioCadastro(final Model model) {
        if (!model.containsAttribute("novoUsuario")) {
            model.addAttribute("novoUsuario", new NovoUsuario(null, null, null, null, null));
        }
        model.addAttribute("perfis", perfisDisponiveis());

        return "cadastro";
    }

    @PostMapping("/cadastro")
    public String cadastrar(@Valid @ModelAttribute("novoUsuario") final NovoUsuario novoUsuario,
                            final BindingResult validacao,
                            final Model model,
                            final RedirectAttributes atributos) {
        if (validacao.hasErrors()) {
            model.addAttribute("perfis", perfisDisponiveis());
            return "cadastro";
        }

        final CriarUsuarioUseCase.Resultado resultado;
        try {
            resultado = criarUsuario.execute(novoUsuario);
        } catch (final NegocioException excecao) {
            validacao.rejectValue(campoDoErro(excecao), excecao.getErro().getCodigo(), traduzir(excecao));
            model.addAttribute("perfis", perfisDisponiveis());
            return "cadastro";
        }

        atributos.addFlashAttribute("aviso", resultado.aguardandoAprovacao()
                ? "Cadastro recebido. O perfil de responsável precisa ser liberado por um "
                        + "administrador, e você receberá aviso quando isso acontecer."
                : "Cadastro concluído. Você já pode entrar.");

        return "redirect:/login";
    }

    private PerfilUsuario[] perfisDisponiveis() {
        return Arrays.stream(PerfilUsuario.values())
                .filter(PerfilUsuario::disponivelNoAutocadastro)
                .toArray(PerfilUsuario[]::new);
    }

    private String campoDoErro(final NegocioException excecao) {
        return switch (excecao.getErro().getCodigo()) {
            case "USU-003" -> "confirmacaoSenha";
            case "USU-004" -> "perfil";
            default -> "email";
        };
    }

    private String traduzir(final NegocioException excecao) {
        return messageSource.getMessage(excecao.getErro().getChaveMensagem(), null,
                excecao.getErro().getChaveMensagem(), PT_BR);
    }
}
