package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.config.SendHemoscProperties;
import br.univille.sendhemosc.domain.dto.DoadorListado;
import br.univille.sendhemosc.domain.dto.FiltroDoador;
import br.univille.sendhemosc.domain.dto.NovaDoacao;
import br.univille.sendhemosc.domain.dto.ResultadoConvocacao;
import br.univille.sendhemosc.domain.dto.ResultadoRegistroDoacao;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.usecase.doador.ListarDoadoresUseCase;
import br.univille.sendhemosc.usecase.doador.RegistrarDoacaoUseCase;
import br.univille.sendhemosc.usecase.notificacao.ConvocarSelecionadosUseCase;
import br.univille.sendhemosc.usecase.notificacao.LiberarContatoUseCase;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.format.annotation.DateTimeFormat;
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
 *
 * <p>Toda acao devolve a lista com o filtro que estava valendo. Os formularios carregam os
 * mesmos parametros da busca e o redirecionamento os recompoe: sem isso, quem filtrasse por
 * um tipo raro perderia o recorte a cada convocacao e teria de refiltrar.</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DoadorViewAdapter {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final ListarDoadoresUseCase listarDoadores;
    private final ConvocarSelecionadosUseCase convocarSelecionados;
    private final RegistrarDoacaoUseCase registrarDoacao;
    private final LiberarContatoUseCase liberarContato;
    private final SendHemoscProperties properties;
    private final MessageSource messageSource;

    @GetMapping("/doadores")
    public String listar(@RequestParam(required = false) final String busca,
                         @RequestParam(required = false) final String tipo,
                         @RequestParam(defaultValue = "false") final boolean apenasAptos,
                         @RequestParam(defaultValue = "false") final boolean apenasComConsentimento,
                         final Model model) {
        final FiltroDoador filtro = montarFiltro(busca, tipo, apenasAptos, apenasComConsentimento);
        final List<DoadorListado> doadores = listarDoadores.execute(filtro);

        model.addAttribute("doadores", doadores);
        model.addAttribute("filtro", filtro);
        model.addAttribute("tiposSanguineos", TipoSanguineo.values());
        model.addAttribute("totalConvocaveis", doadores.stream().filter(DoadorListado::convocavel).count());
        model.addAttribute("prazoTetoDias", properties.notificacao().prazoTetoDias());

        return "doadores";
    }

    @PostMapping("/doadores/convocar")
    public String convocarSelecao(@RequestParam(required = false) final Set<Long> selecionados,
                                  @RequestParam(required = false) final String busca,
                                  @RequestParam(required = false) final String tipo,
                                  @RequestParam(defaultValue = "false") final boolean apenasAptos,
                                  @RequestParam(defaultValue = "false") final boolean apenasComConsentimento,
                                  final RedirectAttributes atributos) {
        if (selecionados == null || selecionados.isEmpty()) {
            atributos.addFlashAttribute("erro", "Selecione ao menos um doador.");
        } else {
            informar(convocarSelecionados.execute(selecionados), selecionados.size(), atributos);
        }

        return devolverParaLista(atributos, busca, tipo, apenasAptos, apenasComConsentimento);
    }

    @PostMapping("/doadores/{id}/convocar")
    public String convocarUm(@PathVariable final Long id,
                             @RequestParam(required = false) final String busca,
                             @RequestParam(required = false) final String tipo,
                             @RequestParam(defaultValue = "false") final boolean apenasAptos,
                             @RequestParam(defaultValue = "false") final boolean apenasComConsentimento,
                             final RedirectAttributes atributos) {
        informar(convocarSelecionados.execute(Set.of(id)), 1, atributos);

        return devolverParaLista(atributos, busca, tipo, apenasAptos, apenasComConsentimento);
    }

    @PostMapping("/doadores/{id}/doacao")
    public String registrarDoacao(@PathVariable final Long id,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                  final LocalDate dataDoacao,
                                  @RequestParam(required = false) final String localColeta,
                                  @RequestParam(required = false) final String busca,
                                  @RequestParam(required = false) final String tipo,
                                  @RequestParam(defaultValue = "false") final boolean apenasAptos,
                                  @RequestParam(defaultValue = "false") final boolean apenasComConsentimento,
                                  final RedirectAttributes atributos) {
        try {
            final ResultadoRegistroDoacao resultado = registrarDoacao
                    .execute(new NovaDoacao(id, dataDoacao, localColeta, null));

            atributos.addFlashAttribute("aviso", montarConfirmacao(resultado));
        } catch (final NegocioException excecao) {
            atributos.addFlashAttribute("erro", messageSource.getMessage(
                    excecao.getErro().getChaveMensagem(), null,
                    excecao.getErro().getChaveMensagem(), PT_BR));
        }

        return devolverParaLista(atributos, busca, tipo, apenasAptos, apenasComConsentimento);
    }

    @PostMapping("/doadores/{id}/liberar-contato")
    public String liberarContato(@PathVariable final Long id,
                                 @RequestParam(required = false) final String busca,
                                 @RequestParam(required = false) final String tipo,
                                 @RequestParam(defaultValue = "false") final boolean apenasAptos,
                                 @RequestParam(defaultValue = "false") final boolean apenasComConsentimento,
                                 final RedirectAttributes atributos) {
        try {
            final String nome = liberarContato.execute(id);
            atributos.addFlashAttribute("aviso", "Limite de contato de %s liberado: as convocações anteriores "
                    .formatted(nome) + "deixaram de contar, e a pessoa pode ser convocada de novo.");
        } catch (final NegocioException excecao) {
            atributos.addFlashAttribute("erro", messageSource.getMessage(
                    excecao.getErro().getChaveMensagem(), null,
                    excecao.getErro().getChaveMensagem(), PT_BR));
        }

        return devolverParaLista(atributos, busca, tipo, apenasAptos, apenasComConsentimento);
    }

    private FiltroDoador montarFiltro(final String busca, final String tipo,
                                      final boolean apenasAptos, final boolean apenasComConsentimento) {
        final TipoSanguineo tipoSanguineo = tipo == null || tipo.isBlank()
                ? null
                : TipoSanguineo.doSigla(tipo);

        return new FiltroDoador(busca, tipoSanguineo, apenasAptos, apenasComConsentimento);
    }

    /**
     * Recompoe a consulta no endereco de volta, para que a acao devolva a lista como ela
     * estava. So entram os criterios em uso: sem filtro algum o destino continua sendo
     * /doadores, sem cauda de parametros vazios.
     *
     * @param atributos destino dos parametros do redirecionamento
     * @param busca trecho de nome ou e-mail em uso
     * @param tipo sigla do tipo sanguineo em uso
     * @param apenasAptos recorte de quem pode doar hoje
     * @param apenasComConsentimento recorte de quem autorizou contato
     * @return redirecionamento para a listagem, com o filtro recomposto
     */
    private String devolverParaLista(final RedirectAttributes atributos, final String busca,
                                     final String tipo, final boolean apenasAptos,
                                     final boolean apenasComConsentimento) {
        if (busca != null && !busca.isBlank()) {
            atributos.addAttribute("busca", busca);
        }
        if (tipo != null && !tipo.isBlank()) {
            atributos.addAttribute("tipo", tipo);
        }
        if (apenasAptos) {
            atributos.addAttribute("apenasAptos", true);
        }
        if (apenasComConsentimento) {
            atributos.addAttribute("apenasComConsentimento", true);
        }

        return "redirect:/doadores";
    }

    /**
     * Monta a confirmacao reunindo o que a equipe precisa saber de imediato: se alguma
     * convocacao foi encerrada, quando a pessoa volta a poder doar, e o alerta quando o
     * sistema a considerava inapta.
     *
     * @param resultado desfecho do registro
     * @return texto exibido na tela
     */
    private String montarConfirmacao(final ResultadoRegistroDoacao resultado) {
        final StringBuilder texto = new StringBuilder("Doação de %s registrada."
                .formatted(resultado.nomeDoador()));

        if (resultado.convocacoesFechadas() > 0) {
            texto.append(" %d convocação(ões) passaram a constar como atendidas."
                    .formatted(resultado.convocacoesFechadas()));
        }

        texto.append(" Volta a poder doar em %s.".formatted(resultado.proximaDataApta()));

        if (!resultado.estavaApto()) {
            texto.append(" Atenção: o sistema a considerava inapta nessa data.");
        }

        return texto.toString();
    }

    private void informar(final ResultadoConvocacao resultado, final int selecionados,
                          final RedirectAttributes atributos) {
        if (resultado.totalElegiveis() == 0) {
            atributos.addFlashAttribute("erro", recusaSemElegiveis(resultado, selecionados));
        } else if (resultado.totalFalhas() > 0) {
            atributos.addFlashAttribute("erro",
                    "%d convocação(ões) enviada(s), %d falhou(aram). Verifique o envio de e-mail."
                            .formatted(resultado.totalEnviados(), resultado.totalFalhas()));
        } else {
            atributos.addFlashAttribute("aviso",
                    "%d convocação(ões) enviada(s).".formatted(resultado.totalEnviados()));
        }
    }

    private String recusaSemElegiveis(final ResultadoConvocacao resultado, final int selecionados) {
        if (resultado.totalRetidos() > 0) {
            return selecionados == 1
                    ? "Este doador foi convocado há pouco tempo ou não respondeu às últimas convocações."
                    : "Nenhum dos %d selecionados pode ser convocado agora: os aptos foram convocados há "
                            .formatted(selecionados) + "pouco tempo ou não responderam às últimas convocações.";
        }

        return selecionados == 1
                ? "Este doador não está apto ou não autorizou receber convocações."
                : "Nenhum dos %d selecionados está apto e com autorização para receber.".formatted(selecionados);
    }
}
