package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.adapter.outbound.email.EnvioDeEmailRouter;
import br.univille.sendhemosc.config.SendHemoscProperties;
import br.univille.sendhemosc.domain.dto.AjusteDisparoAutomatico;
import br.univille.sendhemosc.domain.dto.ConfiguracaoDisparoAutomatico;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.usecase.notificacao.ConfigurarDisparoAutomaticoUseCase;
import br.univille.sendhemosc.usecase.notificacao.ExecutarDisparoAutomaticoUseCase;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Tela do disparo automatico: liga, desliga e configura a rodada, e mostra quem ela convocaria.
 *
 * <p>Restrita ao responsavel e ao administrador nas regras de acesso, como tudo que manda e-mail.
 * A previa usa o mesmo plano que a rodada executa, para que ligar nao seja um salto no escuro.</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DisparoAutomaticoViewAdapter {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    /** Opcoes de frequencia oferecidas na tela, em dias. */
    private static final List<Integer> FREQUENCIAS = List.of(1, 2, 3, 7);

    private static final long MILISSEGUNDOS_POR_MINUTO = 60_000L;

    private final ConfigurarDisparoAutomaticoUseCase configurarDisparo;
    private final ExecutarDisparoAutomaticoUseCase executarDisparo;
    private final EnvioDeEmailRouter roteadorDeEnvio;
    private final SendHemoscProperties properties;
    private final MessageSource messageSource;

    @Value("${sendhemosc.disparo-automatico.verificacao-ms:600000}")
    private long verificacaoMs;

    @GetMapping("/disparo-automatico")
    public String exibir(final Model model) {
        final LocalDateTime agora = LocalDateTime.now(ConfiguracaoDisparoAutomatico.FUSO);
        final ConfiguracaoDisparoAutomatico configuracao = configurarDisparo.consultar();

        // Depois de um erro de validacao o formulario volta com o que foi digitado, e nao com o gravado.
        if (!model.containsAttribute("ajuste")) {
            model.addAttribute("ajuste", AjusteDisparoAutomatico.de(configuracao));
        }

        model.addAttribute("configuracao", configuracao);
        model.addAttribute("agora", agora);
        model.addAttribute("proximaRodada", configuracao.proximaRodada(agora));
        model.addAttribute("plano", executarDisparo.planejar(configuracao, agora.toLocalDate()));
        model.addAttribute("tiposSanguineos", TipoSanguineo.values());
        model.addAttribute("frequencias", FREQUENCIAS);
        model.addAttribute("envioReal", roteadorDeEnvio.isEntregando());
        model.addAttribute("intervaloReenvioDias", properties.notificacao().intervaloReenvioDias());
        model.addAttribute("maxConvocacoesSemResposta", properties.notificacao().maxConvocacoesSemResposta());
        model.addAttribute("prazoTetoDias", properties.notificacao().prazoTetoDias());
        // Arredonda para cima: com conferencia abaixo de um minuto a tela diria "em ate 0 minutos".
        model.addAttribute("verificacaoMinutos",
                Math.max(1L, (verificacaoMs + MILISSEGUNDOS_POR_MINUTO - 1) / MILISSEGUNDOS_POR_MINUTO));

        return "disparo-automatico";
    }

    @PostMapping("/disparo-automatico")
    public String salvar(@RequestParam(defaultValue = "false") final boolean ativo,
                         @RequestParam(required = false) final List<String> tipos,
                         @RequestParam final int limitePorRodada,
                         @RequestParam final int horaInicio,
                         @RequestParam final int horaFim,
                         @RequestParam final int intervaloDias,
                         final Principal autenticado,
                         final RedirectAttributes atributos) {
        final AjusteDisparoAutomatico ajuste = new AjusteDisparoAutomatico(ativo, paraTipos(tipos),
                limitePorRodada, horaInicio, horaFim, intervaloDias);

        try {
            final ConfigurarDisparoAutomaticoUseCase.Alteracao alteracao =
                    configurarDisparo.salvar(ajuste, autenticado.getName());

            atributos.addFlashAttribute("aviso", switch (alteracao) {
                case NENHUMA -> "Nada foi alterado.";
                case LIGADO -> "Disparo automático ligado. Confira abaixo quem a próxima rodada convocaria.";
                case DESLIGADO -> "Disparo automático desligado. Nenhuma rodada acontece até alguém ligar de novo.";
                case ALTERADO -> "Configuração do disparo automático salva.";
            });
        } catch (final NegocioException excecao) {
            atributos.addFlashAttribute("ajuste", ajuste);
            atributos.addFlashAttribute("erro", messageSource.getMessage(
                    excecao.getErro().getChaveMensagem(), null,
                    excecao.getErro().getChaveMensagem(), PT_BR));
        }

        return "redirect:/disparo-automatico";
    }

    private Set<TipoSanguineo> paraTipos(final List<String> siglas) {
        final Set<TipoSanguineo> tipos = EnumSet.noneOf(TipoSanguineo.class);

        if (siglas != null) {
            siglas.stream().map(TipoSanguineo::doSigla).forEach(tipos::add);
        }

        return tipos;
    }
}
