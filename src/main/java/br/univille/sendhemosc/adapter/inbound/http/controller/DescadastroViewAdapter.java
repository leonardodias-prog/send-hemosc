package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.usecase.notificacao.DescadastrarDoadorUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Link de descadastro presente em todo e-mail enviado. Exigencia da LGPD: o titular precisa
 * conseguir revogar o consentimento sem depender de contato com a instituicao.
 *
 * <p>Abrir o link mostra a pagina, e o cancelamento sai do botao dela, com um clique. Antes,
 * abrir ja cancelava: filtros de e-mail que abrem links sozinhos para inspecionar
 * descadastravam o doador sem ele saber.</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DescadastroViewAdapter {

    private final DescadastrarDoadorUseCase descadastrarDoador;

    @GetMapping("/descadastro/{token}")
    public String pagina(@PathVariable final String token, final Model model) {
        model.addAttribute("situacao", descadastrarDoador.consultar(token));
        model.addAttribute("token", token);

        return "descadastro";
    }

    @PostMapping("/descadastro/{token}")
    public String descadastrar(@PathVariable final String token, final RedirectAttributes atributos) {
        atributos.addFlashAttribute("efetivado", descadastrarDoador.execute(token));

        // Redireciona para que atualizar a pagina nao reenvie o pedido.
        return "redirect:/descadastro/" + token;
    }
}
