package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.config.SendHemoscProperties;
import br.univille.sendhemosc.domain.dto.DoadorListado;
import br.univille.sendhemosc.domain.dto.FiltroDoador;
import br.univille.sendhemosc.domain.dto.NovaDoacao;
import br.univille.sendhemosc.domain.dto.Pagina;
import br.univille.sendhemosc.domain.dto.ResultadoConvocacao;
import br.univille.sendhemosc.domain.dto.ResultadoRegistroDoacao;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import br.univille.sendhemosc.usecase.doador.ListarDoadoresUseCase;
import br.univille.sendhemosc.usecase.doador.RegistrarDoacaoUseCase;
import br.univille.sendhemosc.usecase.notificacao.ConvocarSelecionadosUseCase;
import br.univille.sendhemosc.usecase.notificacao.LiberarContatoUseCase;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Tela de doadores: busca com filtros e convocacao de uma selecao especifica.
 *
 * <p>E o unico lugar que dispara convocacao manual, por tipo sanguineo ou por selecao
 * especifica — o painel so linka para aqui, com o tipo e a compatibilidade ja preenchidos.
 * Uma selecao especifica atende o doador raro que a equipe conhece, um grupo que ja se dispos,
 * um reforco pontual.</p>
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
    private static final int POR_PAGINA = 25;

    private final ListarDoadoresUseCase listarDoadores;
    private final ConvocarSelecionadosUseCase convocarSelecionados;
    private final RegistrarDoacaoUseCase registrarDoacao;
    private final LiberarContatoUseCase liberarContato;
    private final IDoadorRepositoryPort doadorRepository;
    private final SendHemoscProperties properties;
    private final MessageSource messageSource;

    @GetMapping("/doadores")
    public String listar(@RequestParam(required = false) final String busca,
                         @RequestParam(required = false) final String tipo,
                         @RequestParam(defaultValue = "false") final boolean apenasAptos,
                         @RequestParam(defaultValue = "false") final boolean apenasComConsentimento,
                         @RequestParam(defaultValue = "false") final boolean porCompatibilidade,
                         @RequestParam(defaultValue = "false") final boolean apenasConvocados,
                         @RequestParam(defaultValue = "1") final int pagina,
                         final Model model) {
        final FiltroDoador filtro = montarFiltro(busca, tipo, apenasAptos, apenasComConsentimento,
                porCompatibilidade, apenasConvocados);
        final List<DoadorListado> doadores = listarDoadores.execute(filtro);
        final Pagina<DoadorListado> trecho = Pagina.recortar(doadores, pagina, POR_PAGINA);

        // A aptidao e calculada em Java (ver ListarDoadoresUseCase), entao o recorte vem depois
        // dela. O que pesava era a tela: centenas de linhas, cada uma com os seus botoes.
        model.addAttribute("doadores", trecho.itens());
        model.addAttribute("paginaDoadores", trecho);
        model.addAttribute("urlPaginacao", ParaPaginacao.url(UriComponentsBuilder.fromPath("/doadores")
                .queryParamIfPresent("busca", Optional.ofNullable(busca).filter(b -> !b.isBlank()))
                .queryParamIfPresent("tipo", Optional.ofNullable(tipo).filter(t -> !t.isBlank()))
                .queryParamIfPresent("apenasAptos", Optional.of(true).filter(v -> apenasAptos))
                .queryParamIfPresent("apenasComConsentimento",
                        Optional.of(true).filter(v -> apenasComConsentimento))
                .queryParamIfPresent("porCompatibilidade", Optional.of(true).filter(v -> porCompatibilidade))
                .queryParamIfPresent("apenasConvocados", Optional.of(true).filter(v -> apenasConvocados))));
        model.addAttribute("totalEncontrados", doadores.size());
        model.addAttribute("filtro", filtro);
        model.addAttribute("tiposSanguineos", TipoSanguineo.values());
        model.addAttribute("totalConvocaveis", doadores.stream().filter(DoadorListado::convocavel).count());
        model.addAttribute("prazoTetoDias", properties.notificacao().prazoTetoDias());
        model.addAttribute("inativos", doadorRepository.listarInativos());

        return "doadores";
    }

    @PostMapping("/doadores/convocar")
    public String convocarSelecao(@RequestParam(required = false) final Set<Long> selecionados,
                                  @RequestParam(defaultValue = "false") final boolean todos,
                                  @RequestParam(required = false) final String busca,
                                  @RequestParam(required = false) final String tipo,
                                  @RequestParam(defaultValue = "false") final boolean apenasAptos,
                                  @RequestParam(defaultValue = "false") final boolean apenasComConsentimento,
                                  @RequestParam(defaultValue = "false") final boolean porCompatibilidade,
                                  @RequestParam(defaultValue = "false") final boolean apenasConvocados,
                                  @RequestParam(defaultValue = "1") final int pagina,
                                  final RedirectAttributes atributos) {
        // "todos" convoca quem pode ser convocado em TODAS as paginas do filtro: e o caminho do botao
        // do painel, que antes convocava o tipo inteiro de uma vez. A selecao marcada so alcanca a
        // pagina visivel. Exige tipo sanguineo: sem ele, "todos" seria a base inteira.
        if (todos && (tipo == null || tipo.isBlank())) {
            atributos.addFlashAttribute("erro", "Para convocar todos de uma vez, escolha um tipo sanguíneo.");
            return devolverParaLista(atributos, busca, tipo, apenasAptos, apenasComConsentimento, porCompatibilidade,
                    apenasConvocados, pagina);
        }

        final Set<Long> alvo = todos
                ? listarDoadores.execute(montarFiltro(busca, tipo, apenasAptos, apenasComConsentimento,
                                porCompatibilidade, apenasConvocados)).stream()
                        .filter(DoadorListado::convocavel)
                        .map(DoadorListado::id)
                        .collect(Collectors.toSet())
                : selecionados;

        if (alvo == null || alvo.isEmpty()) {
            atributos.addFlashAttribute("erro", todos
                    ? "Ninguém neste filtro pode ser convocado agora."
                    : "Selecione ao menos um doador.");
        } else {
            informar(convocarSelecionados.execute(alvo), alvo.size(), atributos);
        }

        return devolverParaLista(atributos, busca, tipo, apenasAptos, apenasComConsentimento, porCompatibilidade,
                apenasConvocados, pagina);
    }

    @PostMapping("/doadores/{id}/convocar")
    public String convocarUm(@PathVariable final Long id,
                             @RequestParam(required = false) final String busca,
                             @RequestParam(required = false) final String tipo,
                             @RequestParam(defaultValue = "false") final boolean apenasAptos,
                             @RequestParam(defaultValue = "false") final boolean apenasComConsentimento,
                             @RequestParam(defaultValue = "false") final boolean porCompatibilidade,
                             @RequestParam(defaultValue = "false") final boolean apenasConvocados,
                             @RequestParam(defaultValue = "1") final int pagina,
                             final RedirectAttributes atributos) {
        informar(convocarSelecionados.execute(Set.of(id)), 1, atributos);

        return devolverParaLista(atributos, busca, tipo, apenasAptos, apenasComConsentimento, porCompatibilidade,
                apenasConvocados, pagina);
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
                                  @RequestParam(defaultValue = "false") final boolean porCompatibilidade,
                                  @RequestParam(defaultValue = "false") final boolean apenasConvocados,
                                  @RequestParam(defaultValue = "1") final int pagina,
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

        return devolverParaLista(atributos, busca, tipo, apenasAptos, apenasComConsentimento, porCompatibilidade,
                apenasConvocados, pagina);
    }

    @PostMapping("/doadores/{id}/liberar-contato")
    public String liberarContato(@PathVariable final Long id,
                                 @RequestParam(required = false) final String busca,
                                 @RequestParam(required = false) final String tipo,
                                 @RequestParam(defaultValue = "false") final boolean apenasAptos,
                                 @RequestParam(defaultValue = "false") final boolean apenasComConsentimento,
                                 @RequestParam(defaultValue = "false") final boolean porCompatibilidade,
                                 @RequestParam(defaultValue = "false") final boolean apenasConvocados,
                                 @RequestParam(defaultValue = "1") final int pagina,
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

        return devolverParaLista(atributos, busca, tipo, apenasAptos, apenasComConsentimento, porCompatibilidade,
                apenasConvocados, pagina);
    }

    private FiltroDoador montarFiltro(final String busca, final String tipo, final boolean apenasAptos,
                                      final boolean apenasComConsentimento, final boolean porCompatibilidade,
                                      final boolean apenasConvocados) {
        final TipoSanguineo tipoSanguineo = tipo == null || tipo.isBlank()
                ? null
                : TipoSanguineo.doSigla(tipo);

        return new FiltroDoador(busca, tipoSanguineo, apenasAptos, apenasComConsentimento, porCompatibilidade,
                apenasConvocados);
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
     * @param porCompatibilidade recorte por compatibilidade com o tipo, em vez do tipo exato
     * @param apenasConvocados recorte de quem foi convocado e ainda nao respondeu
     * @param pagina pagina em que a pessoa estava
     * @return redirecionamento para a listagem, com o filtro recomposto
     */
    private String devolverParaLista(final RedirectAttributes atributos, final String busca,
                                     final String tipo, final boolean apenasAptos,
                                     final boolean apenasComConsentimento, final boolean porCompatibilidade,
                                     final boolean apenasConvocados, final int pagina) {
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
        if (porCompatibilidade) {
            atributos.addAttribute("porCompatibilidade", true);
        }
        if (apenasConvocados) {
            atributos.addAttribute("apenasConvocados", true);
        }
        if (pagina > 1) {
            atributos.addAttribute("pagina", pagina);
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
