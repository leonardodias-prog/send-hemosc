package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.domain.dto.DadosDoTitular;
import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import br.univille.sendhemosc.usecase.doador.ExcluirDoadorUseCase;
import br.univille.sendhemosc.usecase.doador.ExportarDadosDoTitularUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Pagina do titular: onde o doador exerce, sem depender da equipe, os direitos da LGPD sobre os
 * proprios dados — obter uma copia (art. 18, II e V) e pedir a eliminacao (art. 18, VI).
 *
 * <p>O doador nao tem conta no sistema. Ele se identifica pelo mesmo token do link de
 * cancelamento, que so chega a quem recebe os e-mails. Por isso a pagina e publica.</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class TitularViewAdapter {

    private final IDoadorRepositoryPort doadorRepository;
    private final ExportarDadosDoTitularUseCase exportarDados;
    private final ExcluirDoadorUseCase excluirDoador;

    @GetMapping("/meus-dados/{token}")
    public String pagina(@PathVariable final String token, final Model model) {
        doadorRepository.buscarPorToken(token).ifPresent(doador -> model.addAttribute("doador", doador));
        model.addAttribute("token", token);

        return "meus-dados";
    }

    @GetMapping("/meus-dados/{token}/exportar")
    @ResponseBody
    public ResponseEntity<DadosDoTitular> exportar(@PathVariable final String token) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("meus-dados-send-hemosc.json")
                        .build()
                        .toString())
                .body(exportarDados.porToken(token));
    }

    @PostMapping("/meus-dados/{token}/excluir")
    public String excluir(@PathVariable final String token,
                          @RequestParam(defaultValue = "false") final boolean confirmacao,
                          final RedirectAttributes atributos) {
        if (!confirmacao) {
            atributos.addFlashAttribute("erro", "Marque a confirmação para excluir seus dados.");
        } else {
            excluirDoador.porToken(token);
            atributos.addFlashAttribute("excluido", true);
        }

        // Redireciona para que atualizar a pagina nao reenvie o pedido de exclusao.
        return "redirect:/meus-dados/" + token;
    }
}
