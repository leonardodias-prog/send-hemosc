package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.domain.dto.ResultadoConvocacao;
import br.univille.sendhemosc.domain.dto.SituacaoEstoque;
import br.univille.sendhemosc.domain.enums.OrigemNotificacao;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.port.outbound.IEstoqueRepositoryPort;
import br.univille.sendhemosc.usecase.estoque.ListarSituacaoEstoqueUseCase;
import br.univille.sendhemosc.usecase.notificacao.ConvocarDoadoresUseCase;
import java.util.List;
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
 * Painel web da equipe de captacao. E a tela usada para demonstrar o sistema aos profissionais
 * da instituicao: mostra a situacao do estoque por tipo sanguineo e permite disparar a
 * convocacao segmentada manualmente.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PainelViewAdapter {

    private final ListarSituacaoEstoqueUseCase listarSituacaoEstoque;
    private final ConvocarDoadoresUseCase convocarDoadores;
    private final IEstoqueRepositoryPort estoqueRepository;

    @GetMapping("/")
    public String painel(final Model model) {
        final List<SituacaoEstoque> situacoes = listarSituacaoEstoque.execute();

        model.addAttribute("situacoes", situacoes);
        model.addAttribute("totalEmFalta", situacoes.stream()
                .filter(situacao -> situacao.nivel().isExigeConvocacao())
                .count());

        return "painel";
    }

    @PostMapping("/convocar/{sigla}")
    public String convocar(@PathVariable final String sigla, final RedirectAttributes atributos) {
        final TipoSanguineo tipo = TipoSanguineo.doSigla(sigla);
        final ResultadoConvocacao resultado = convocarDoadores.execute(tipo, OrigemNotificacao.MANUAL, true);

        atributos.addFlashAttribute("aviso", resultado.totalEnviados() == 0
                ? "Nenhum doador apto encontrado para %s no momento.".formatted(tipo.getSigla())
                : "%d doador(es) apto(s) convocado(s) para %s. Em modo de demonstração nenhum e-mail sai de verdade."
                        .formatted(resultado.totalEnviados(), tipo.getSigla()));

        return "redirect:/";
    }

    @PostMapping("/estoque/{sigla}")
    public String atualizarEstoque(@PathVariable final String sigla,
                                   @RequestParam final int quantidadeBolsas,
                                   final RedirectAttributes atributos) {
        final TipoSanguineo tipo = TipoSanguineo.doSigla(sigla);
        estoqueRepository.atualizarQuantidade(tipo, Math.max(0, quantidadeBolsas));

        atributos.addFlashAttribute("aviso",
                "Estoque de %s atualizado para %d bolsas.".formatted(tipo.getSigla(), quantidadeBolsas));

        return "redirect:/";
    }
}
