package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.usecase.notificacao.DescadastrarDoadorUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Link de descadastro presente em todo e-mail enviado. Exigencia da LGPD: o titular precisa
 * conseguir revogar o consentimento sem depender de contato com a instituicao.
 *
 * <p>A pagina e um template como as demais. Antes era HTML montado aqui dentro, com duas
 * linhas de texto e saida nenhuma: quem cancelava ficava sem saber o que acontecera com o
 * cadastro nem como voltar a receber convocacoes.</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DescadastroViewAdapter {

    private final DescadastrarDoadorUseCase descadastrarDoador;

    @GetMapping("/descadastro/{token}")
    public String descadastrar(@PathVariable final String token, final Model model) {
        model.addAttribute("efetivado", descadastrarDoador.execute(token));
        model.addAttribute("token", token);

        return "descadastro";
    }
}
