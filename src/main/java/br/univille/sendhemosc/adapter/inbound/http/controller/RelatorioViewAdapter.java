package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.usecase.notificacao.GerarRelatorioConversaoUseCase;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Tela de relatorios. Comeca com o de conversao: quantos convocados viraram doacao, por tipo
 * sanguineo e periodo.
 */
@Controller
@RequiredArgsConstructor
public class RelatorioViewAdapter {

    /** Periodo mostrado quando a pessoa ainda nao escolheu um. */
    private static final int PERIODO_PADRAO_DIAS = 30;

    private final GerarRelatorioConversaoUseCase gerarRelatorioConversao;

    @GetMapping("/relatorios/conversao")
    public String conversao(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                            final LocalDate dataInicio,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                            final LocalDate dataFim,
                            final Model model) {
        final LocalDate fim = dataFim != null ? dataFim : LocalDate.now();
        final LocalDate inicio = dataInicio != null ? dataInicio : fim.minusDays(PERIODO_PADRAO_DIAS - 1);

        model.addAttribute("relatorio", gerarRelatorioConversao.execute(inicio, fim));

        return "relatorio-conversao";
    }
}
