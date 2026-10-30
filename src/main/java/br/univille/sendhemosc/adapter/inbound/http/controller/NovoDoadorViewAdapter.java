package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.domain.dto.NovoDoador;
import br.univille.sendhemosc.domain.enums.Sexo;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.usecase.doador.CriarDoadorUseCase;
import jakarta.validation.Valid;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Cadastro de doador, em tela propria a partir da tela de doadores. Depois de salvar, a pessoa
 * volta ao formulario vazio com a confirmacao: quem cadastra varios seguidos nao precisa navegar.
 */
@Controller
@RequiredArgsConstructor
public class NovoDoadorViewAdapter {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final String TELA = "redirect:/doadores/novo";

    private final CriarDoadorUseCase criarDoador;
    private final MessageSource messageSource;

    @GetMapping("/doadores/novo")
    public String formulario(final Model model) {
        if (!model.containsAttribute("novoDoador")) {
            model.addAttribute("novoDoador", new NovoDoador(null, null, null, null, null, null, null, true));
        }
        model.addAttribute("tiposSanguineos", TipoSanguineo.values());
        model.addAttribute("sexos", Sexo.values());

        return "doador-novo";
    }

    @PostMapping("/doadores")
    public String cadastrar(@Valid @ModelAttribute("novoDoador") final NovoDoador novoDoador,
                            final BindingResult validacao,
                            final RedirectAttributes atributos) {
        if (validacao.hasErrors()) {
            return devolverAoFormulario(novoDoador, validacao, atributos);
        }

        try {
            criarDoador.execute(novoDoador);
        } catch (final NegocioException excecao) {
            validacao.rejectValue("email", excecao.getErro().getCodigo(),
                    messageSource.getMessage(excecao.getErro().getChaveMensagem(), null,
                            excecao.getErro().getChaveMensagem(), PT_BR));
            return devolverAoFormulario(novoDoador, validacao, atributos);
        }

        atributos.addFlashAttribute("aviso",
                "Doador %s cadastrado com o e-mail %s.".formatted(novoDoador.nome(), novoDoador.email()));

        return TELA;
    }

    /**
     * Redireciona em vez de renderizar, para que atualizar a pagina nao reenvie o formulario. Os
     * dados preenchidos voltam junto, para nao obrigar a redigitar.
     */
    private String devolverAoFormulario(final NovoDoador novoDoador, final BindingResult validacao,
                                        final RedirectAttributes atributos) {
        atributos.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + "novoDoador", validacao);
        atributos.addFlashAttribute("novoDoador", novoDoador);

        return TELA;
    }
}
