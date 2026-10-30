package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.domain.dto.SituacaoEstoque;
import br.univille.sendhemosc.domain.enums.NivelEstoque;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import br.univille.sendhemosc.usecase.estoque.AtualizarEstoqueUseCase;
import br.univille.sendhemosc.usecase.estoque.ListarSituacaoEstoqueUseCase;
import br.univille.sendhemosc.usecase.notificacao.ConvocarTiposEmFaltaUseCase;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Painel da equipe de captacao: a situacao do estoque por tipo sanguineo e um unico botao de
 * convocacao, para todos os tipos em falta. Convocar um tipo ou pessoas especificas e na tela de
 * doadores; o cadastro de doador, o relatorio de movimentacoes e o envio de e-mail tem as suas
 * proprias telas.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PainelViewAdapter {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final ListarSituacaoEstoqueUseCase listarSituacaoEstoque;
    private final AtualizarEstoqueUseCase atualizarEstoque;
    private final ConvocarTiposEmFaltaUseCase convocarTiposEmFalta;
    private final IDoadorRepositoryPort doadorRepository;
    private final MessageSource messageSource;

    @GetMapping("/")
    public String painel(final Authentication autenticado, final Model model) {
        final List<SituacaoEstoque> situacoes = listarSituacaoEstoque.execute();

        model.addAttribute("situacoes", situacoes);
        model.addAttribute("totalEmFalta", situacoes.stream()
                .filter(situacao -> situacao.nivel().isExigeConvocacao())
                .count());
        model.addAttribute("totalCriticos", situacoes.stream()
                .filter(situacao -> situacao.nivel() == NivelEstoque.CRITICO)
                .count());
        model.addAttribute("totalBolsas", situacoes.stream().mapToInt(SituacaoEstoque::quantidadeBolsas).sum());
        model.addAttribute("totalDoadores", doadorRepository.contar());

        // O plano so interessa a quem pode convocar: os demais nao veem o botao.
        if (podeConvocar(autenticado)) {
            model.addAttribute("planoGeral", convocarTiposEmFalta.planejar(LocalDate.now()));
        }

        return "painel";
    }

    @PostMapping("/convocar/em-falta")
    public String convocarEmFalta(final RedirectAttributes atributos) {
        final ConvocarTiposEmFaltaUseCase.Resultado resultado = convocarTiposEmFalta.execute(LocalDate.now());

        if (resultado.tipos() == 0) {
            atributos.addFlashAttribute("aviso", "Nenhum tipo sanguíneo está em nível baixo: ninguém foi convocado.");
        } else if (resultado.falhas() > 0) {
            atributos.addFlashAttribute("erro", "%d convocação(ões) enviada(s) e %d falha(s). Confira o envio de e-mail."
                    .formatted(resultado.enviados(), resultado.falhas()));
        } else if (resultado.enviados() == 0) {
            atributos.addFlashAttribute("aviso", "Ninguém foi convocado: os aptos foram convocados há pouco tempo "
                    + "ou não responderam às últimas convocações.");
        } else {
            atributos.addFlashAttribute("aviso", "%d doador(es) convocado(s) para %d tipo(s) em nível baixo."
                    .formatted(resultado.enviados(), resultado.tipos()));
        }

        return "redirect:/";
    }

    @PostMapping("/estoque/{sigla}")
    public String atualizarEstoque(@PathVariable final String sigla,
                                   @RequestParam final int quantidadeBolsas,
                                   @RequestParam(required = false) final Integer capacidadeAlvo,
                                   final RedirectAttributes atributos) {
        final TipoSanguineo tipo = TipoSanguineo.doSigla(sigla);

        try {
            final boolean alterou = atualizarEstoque.execute(tipo, quantidadeBolsas, capacidadeAlvo).isPresent();

            atributos.addFlashAttribute("aviso", alterou
                    ? "Estoque de %s atualizado.".formatted(tipo.getSigla())
                    : "Estoque de %s sem alteração: os valores já eram esses.".formatted(tipo.getSigla()));
        } catch (final NegocioException excecao) {
            atributos.addFlashAttribute("erro", messageSource.getMessage(excecao.getErro().getChaveMensagem(),
                    null, excecao.getErro().getChaveMensagem(), PT_BR));
        }

        return "redirect:/";
    }

    private boolean podeConvocar(final Authentication autenticado) {
        return autenticado != null && autenticado.getAuthorities().stream()
                .anyMatch(perfil -> "ROLE_RESPONSAVEL".equals(perfil.getAuthority())
                        || "ROLE_MASTER".equals(perfil.getAuthority()));
    }
}
