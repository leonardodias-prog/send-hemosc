package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.config.SegurancaProperties;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.exception.UsuarioErrorsMessage;
import br.univille.sendhemosc.usecase.usuario.RedefinirSenhaPorLinkUseCase;
import br.univille.sendhemosc.usecase.usuario.SolicitarRecuperacaoDeSenhaUseCase;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Recuperacao de senha para quem nao consegue entrar: pedir o link e, com ele, criar a senha
 * nova. Tudo aqui e publico, porque quem chega nao esta autenticado.
 */
@Controller
@RequiredArgsConstructor
public class RecuperacaoDeSenhaViewAdapter {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final SolicitarRecuperacaoDeSenhaUseCase solicitarRecuperacao;
    private final RedefinirSenhaPorLinkUseCase redefinirSenha;
    private final SegurancaProperties seguranca;
    private final MessageSource messageSource;

    @GetMapping("/senha/esqueci")
    public String formularioDePedido(final Model model) {
        model.addAttribute("validadeMinutos", seguranca.recuperacaoSenha().validadeMinutos());

        return "senha-esqueci";
    }

    @PostMapping("/senha/esqueci")
    public String pedirLink(@RequestParam(required = false) final String email, final RedirectAttributes atributos) {
        solicitarRecuperacao.execute(email);

        // A mesma resposta exista a conta ou nao: a tela nao pode revelar quem tem cadastro.
        atributos.addFlashAttribute("aviso", ("Se houver uma conta ativa com este e-mail, enviamos um link para "
                + "criar uma senha nova. Ele vale por %d minutos; confira também a caixa de spam.")
                .formatted(seguranca.recuperacaoSenha().validadeMinutos()));

        return "redirect:/login";
    }

    @GetMapping("/senha/redefinir/{token}")
    public String formularioDeSenhaNova(@PathVariable final String token, final Model model) {
        redefinirSenha.consultar(token).ifPresent(conta -> model.addAttribute("conta", conta));
        model.addAttribute("token", token);

        return "senha-redefinir";
    }

    @PostMapping("/senha/redefinir/{token}")
    public String redefinir(@PathVariable final String token,
                            @RequestParam(required = false) final String novaSenha,
                            @RequestParam(required = false) final String confirmacao,
                            final RedirectAttributes atributos) {
        try {
            redefinirSenha.execute(token, novaSenha, confirmacao);
        } catch (final NegocioException excecao) {
            // Link que nao serve volta a propria pagina, que mostra como pedir outro. Erro de
            // senha volta ao formulario, com o link ainda valido.
            if (excecao.getErro() != UsuarioErrorsMessage.RECUPERACAO_INVALIDA) {
                atributos.addFlashAttribute("erro", messageSource.getMessage(
                        excecao.getErro().getChaveMensagem(), null,
                        excecao.getErro().getChaveMensagem(), PT_BR));
            }

            return "redirect:/senha/redefinir/" + token;
        }

        atributos.addFlashAttribute("aviso", "Senha nova criada. Entre com ela.");

        return "redirect:/login";
    }
}
