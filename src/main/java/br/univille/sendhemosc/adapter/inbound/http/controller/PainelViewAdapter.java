package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.adapter.outbound.email.EnvioDeEmailRouter;
import br.univille.sendhemosc.config.ModoEnvioEmail;
import br.univille.sendhemosc.domain.dto.NovoDoador;
import br.univille.sendhemosc.domain.dto.ResultadoConvocacao;
import br.univille.sendhemosc.domain.dto.SituacaoEstoque;
import br.univille.sendhemosc.domain.enums.OrigemNotificacao;
import br.univille.sendhemosc.domain.enums.Sexo;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.IEstoqueRepositoryPort;
import br.univille.sendhemosc.usecase.doador.CriarDoadorUseCase;
import br.univille.sendhemosc.usecase.estoque.ListarSituacaoEstoqueUseCase;
import br.univille.sendhemosc.usecase.notificacao.ConvocarDoadoresUseCase;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Painel web da equipe de captacao. E a tela usada para demonstrar o sistema aos profissionais
 * da instituicao: mostra a situacao do estoque por tipo sanguineo, permite cadastrar doadores
 * e disparar a convocacao segmentada manualmente.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PainelViewAdapter {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final ListarSituacaoEstoqueUseCase listarSituacaoEstoque;
    private final ConvocarDoadoresUseCase convocarDoadores;
    private final CriarDoadorUseCase criarDoador;
    private final IEstoqueRepositoryPort estoqueRepository;
    private final IDoadorRepositoryPort doadorRepository;
    private final MessageSource messageSource;
    private final EnvioDeEmailRouter roteadorDeEnvio;

    @Value("${sendhemosc.email.modo:log}")
    private String modoEmail;

    @Value("${sendhemosc.email.destinatario-teste:}")
    private String destinatarioTeste;

    @GetMapping("/")
    public String painel(final Model model) {
        prepararModelo(model);

        if (!model.containsAttribute("novoDoador")) {
            model.addAttribute("novoDoador", formularioVazio());
        }

        return "painel";
    }

    @PostMapping("/doadores")
    public String cadastrarDoador(@Valid @ModelAttribute("novoDoador") final NovoDoador novoDoador,
                                  final BindingResult validacao,
                                  final RedirectAttributes atributos) {
        if (validacao.hasErrors()) {
            return devolverAoFormulario(novoDoador, validacao, atributos);
        }

        try {
            criarDoador.execute(novoDoador);
        } catch (final NegocioException excecao) {
            validacao.rejectValue("email", excecao.getErro().getCodigo(),
                    messageSource.getMessage(excecao.getErro().getChaveMensagem(), null,
                            excecao.getErro().getChaveMensagem(), PT_BR));
            return devolverAoFormulario(novoDoador, validacao, atributos);
        }

        atributos.addFlashAttribute("avisoCadastro",
                "Doador %s cadastrado com o e-mail %s.".formatted(novoDoador.nome(), novoDoador.email()));

        return "redirect:/#cadastro";
    }

    /**
     * Devolve ao formulario, e nao ao topo da pagina. O cadastro fica no fim do painel: quem
     * registra varios doadores seguidos rolava a pagina inteira a cada um. A confirmacao e os
     * erros aparecem junto do formulario, onde a pessoa esta olhando.
     *
     * <p>Redirecionar em vez de renderizar tambem evita o reenvio do formulario quando a
     * pessoa atualiza a pagina.</p>
     *
     * @param novoDoador dados preenchidos, preservados para nao obrigar a redigitar
     * @param validacao erros encontrados
     * @param atributos destino do que sobrevive ao redirecionamento
     * @return redirecionamento para a ancora do formulario
     */
    private String devolverAoFormulario(final NovoDoador novoDoador, final BindingResult validacao,
                                        final RedirectAttributes atributos) {
        atributos.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + "novoDoador", validacao);
        atributos.addFlashAttribute("novoDoador", novoDoador);

        return "redirect:/#cadastro";
    }

    @PostMapping("/convocar/{sigla}")
    public String convocar(@PathVariable final String sigla, final RedirectAttributes atributos) {
        final TipoSanguineo tipo = TipoSanguineo.doSigla(sigla);
        final ResultadoConvocacao resultado = convocarDoadores.execute(tipo, OrigemNotificacao.MANUAL, true);

        atributos.addFlashAttribute("aviso", resultado.totalEnviados() == 0
                ? "Nenhum doador apto encontrado para %s no momento.".formatted(tipo.getSigla())
                : "%d doador(es) apto(s) convocado(s) para %s.".formatted(resultado.totalEnviados(), tipo.getSigla()));

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

    private void prepararModelo(final Model model) {
        final List<SituacaoEstoque> situacoes = listarSituacaoEstoque.execute();

        model.addAttribute("situacoes", situacoes);
        model.addAttribute("totalEmFalta", situacoes.stream()
                .filter(situacao -> situacao.nivel().isExigeConvocacao())
                .count());
        model.addAttribute("totalDoadores", doadorRepository.contar());
        model.addAttribute("tiposSanguineos", TipoSanguineo.values());
        model.addAttribute("sexos", Sexo.values());
        model.addAttribute("envioReal", roteadorDeEnvio.isEntregando());
        model.addAttribute("provedorConfigurado", roteadorDeEnvio.temProvedorConfigurado());
        model.addAttribute("modoEmail", ModoEnvioEmail.de(modoEmail).name().toLowerCase());
        model.addAttribute("destinatarioTeste", destinatarioTeste);
    }

    private NovoDoador formularioVazio() {
        return new NovoDoador(null, null, null, null, null, null, null, true);
    }
}
