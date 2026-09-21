package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.adapter.outbound.email.ControleDeEnvio;
import br.univille.sendhemosc.adapter.outbound.email.EnvioDeEmailRouter;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Liga e desliga o envio de e-mail pela tela. Restrito ao responsavel e ao administrador nas
 * regras de acesso, e toda mudanca fica registrada em auditoria com o nome de quem decidiu.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class EnvioViewAdapter {

    private final ControleDeEnvio controle;
    private final EnvioDeEmailRouter roteador;
    private final IAuditoriaPort auditoria;

    @PostMapping("/envio/{estado}")
    public String alternar(@PathVariable final String estado, final RedirectAttributes atributos) {
        final boolean ligar = "ligar".equals(estado);

        if (!ligar && !"desligar".equals(estado)) {
            atributos.addFlashAttribute("erro", "Ação de envio desconhecida.");
            return "redirect:/";
        }

        if (ligar && !roteador.temProvedorConfigurado()) {
            atributos.addFlashAttribute("erro",
                    "Não há provedor de e-mail configurado neste ambiente. "
                            + "O envio continuará apenas registrando no log.");
            return "redirect:/";
        }

        if (controle.definir(ligar)) {
            auditoria.registrar(ligar ? "ENVIO_LIGADO" : "ENVIO_DESLIGADO",
                    "Envio de e-mail passou a " + (ligar ? "entregar de verdade" : "apenas registrar em log"));
        }

        atributos.addFlashAttribute("aviso", ligar
                ? "Envio de e-mail ligado. As convocações passam a chegar de verdade aos doadores."
                : "Envio de e-mail desligado. As convocações ficam apenas registradas no log.");

        return "redirect:/";
    }
}
