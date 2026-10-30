package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.IEstoqueRepositoryPort;
import br.univille.sendhemosc.usecase.estoque.ListarSituacaoEstoqueUseCase;
import br.univille.sendhemosc.usecase.notificacao.GerarEvolucaoNoPeriodoUseCase;
import br.univille.sendhemosc.usecase.notificacao.GerarRelatorioConversaoUseCase;
import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Tela de relatorios: evolucao de convocacoes e doacoes no periodo, conversao por tipo
 * sanguineo, estoque e base de doadores por tipo, e as ultimas movimentacoes do estoque.
 */
@Controller
@RequiredArgsConstructor
public class RelatorioViewAdapter {

    /** Periodo mostrado quando a pessoa ainda nao escolheu um. */
    private static final int PERIODO_PADRAO_DIAS = 30;
    private static final int MOVIMENTACOES = 20;

    private final GerarRelatorioConversaoUseCase gerarRelatorioConversao;
    private final GerarEvolucaoNoPeriodoUseCase gerarEvolucao;
    private final ListarSituacaoEstoqueUseCase listarSituacaoEstoque;
    private final IDoadorRepositoryPort doadorRepository;
    private final IEstoqueRepositoryPort estoqueRepository;

    @GetMapping("/relatorios")
    public String inicio() {
        return "redirect:/relatorios/conversao";
    }

    @GetMapping("/relatorios/conversao")
    public String conversao(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                            final LocalDate dataInicio,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                            final LocalDate dataFim,
                            @RequestParam(required = false) final Integer dias,
                            final Model model) {
        final LocalDate fim = dataFim != null ? dataFim : LocalDate.now();
        final int periodo = dias != null && dias > 0 ? dias : PERIODO_PADRAO_DIAS;
        final LocalDate inicio = dataInicio != null && dias == null ? dataInicio : fim.minusDays(periodo - 1L);
        final Map<TipoSanguineo, Long> basePorTipo = doadorRepository.contarAtivosPorTipo();

        model.addAttribute("relatorio", gerarRelatorioConversao.execute(inicio, fim));
        model.addAttribute("evolucao", gerarEvolucao.execute(inicio, fim));
        model.addAttribute("dias", dias);
        model.addAttribute("situacoes", listarSituacaoEstoque.execute());
        model.addAttribute("tiposSanguineos", TipoSanguineo.values());
        model.addAttribute("basePorTipo", basePorTipo);
        model.addAttribute("maiorBase", Math.max(1, basePorTipo.values().stream().mapToLong(Long::longValue)
                .max().orElse(0)));
        model.addAttribute("movimentacoes", estoqueRepository.listarMovimentacoes(MOVIMENTACOES));

        return "relatorio-conversao";
    }
}
