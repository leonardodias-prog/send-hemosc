package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.domain.dto.DoadorListado;
import br.univille.sendhemosc.domain.dto.FiltroDoador;
import br.univille.sendhemosc.domain.dto.ResultadoConvocacao;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.usecase.doador.ListarDoadoresUseCase;
import br.univille.sendhemosc.usecase.notificacao.ConvocarSelecionadosUseCase;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Tela de doadores: busca com filtros e convocacao de uma selecao especifica.
 *
 * <p>Complementa a convocacao por tipo sanguineo do painel. Convocar sempre a base inteira
 * atende o caso principal, mas nao os demais: um doador raro que a equipe conhece, um grupo
 * que ja se dispos, um reforco pontual.</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DoadorViewAdapter {

    private final ListarDoadoresUseCase listarDoadores;
    private final ConvocarSelecionadosUseCase convocarSelecionados;

    @GetMapping("/doadores")
    public String listar(@RequestParam(required = false) final String busca,
                         @RequestParam(required = false) final String tipo,
                         @RequestParam(defaultValue = "false") final boolean apenasAptos,
                         @RequestParam(defaultValue = "false") final boolean apenasComConsentimento,
                         final Model model) {
        final TipoSanguineo tipoSanguineo = tipo == null || tipo.isBlank()
                ? null
                : TipoSanguineo.doSigla(tipo);

        final FiltroDoador filtro = new FiltroDoador(busca, tipoSanguineo, apenasAptos, apenasComConsentimento);
        final List<DoadorListado> doadores = listarDoadores.execute(filtro);

        model.addAttribute("doadores", doadores);
        model.addAttribute("filtro", filtro);
        model.addAttribute("tiposSanguineos", TipoSanguineo.values());
        model.addAttribute("totalConvocaveis", doadores.stream().filter(DoadorListado::convocavel).count());

        return "doadores";
    }

    @PostMapping("/doadores/convocar")
    public String convocarSelecao(@RequestParam(required = false) final Set<Long> selecionados,
                                  final RedirectAttributes atributos) {
        if (selecionados == null || selecionados.isEmpty()) {
            atributos.addFlashAttribute("erro", "Selecione ao menos um doador.");
            return "redirect:/doadores";
        }

        return responder(convocarSelecionados.execute(selecionados), selecionados.size(), atributos);
    }

    @PostMapping("/doadores/{id}/convocar")
    public String convocarUm(@PathVariable final Long id, final RedirectAttributes atributos) {
        return responder(convocarSelecionados.execute(Set.of(id)), 1, atributos);
    }

    private String responder(final ResultadoConvocacao resultado, final int selecionados,
                             final RedirectAttributes atributos) {
        if (resultado.totalElegiveis() == 0) {
            atributos.addFlashAttribute("erro", selecionados == 1
                    ? "Este doador não está apto ou não autorizou receber convocações."
                    : "Nenhum dos %d selecionados está apto e com autorização para receber."
                            .formatted(selecionados));
        } else if (resultado.totalFalhas() > 0) {
            atributos.addFlashAttribute("erro",
                    "%d convocação(ões) enviada(s), %d falhou(aram). Verifique o envio de e-mail."
                            .formatted(resultado.totalEnviados(), resultado.totalFalhas()));
        } else {
            atributos.addFlashAttribute("aviso",
                    "%d convocação(ões) enviada(s).".formatted(resultado.totalEnviados()));
        }

        return "redirect:/doadores";
    }
}
