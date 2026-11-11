package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.adapter.outbound.email.EnvioDeEmailRouter;
import br.univille.sendhemosc.domain.dto.ConfiguracaoDisparoAutomatico;
import br.univille.sendhemosc.domain.dto.MinimosDoTipo;
import br.univille.sendhemosc.domain.enums.NivelEstoque;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.exception.EstoqueErrorsMessage;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IAvisoEstoqueRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.IEstoqueRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import br.univille.sendhemosc.usecase.estoque.AvisarEquipeDoEstoqueUseCase;
import br.univille.sendhemosc.usecase.estoque.ClassificarNivelEstoqueUseCase;
import br.univille.sendhemosc.usecase.estoque.ConfigurarAvisoEstoqueUseCase;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Tela do aviso interno de estoque: quando o e-mail sai e os minimos de cada tipo sanguineo.
 * Quem recebe sao os administradores ativos; a tela so os mostra. O aviso nao tem interruptor:
 * daqui so se ajusta a hora do relatorio diario.
 *
 * <p>Restrita ao responsavel e ao administrador nas regras de acesso, como tudo que manda e-mail.
 * O operador continua atualizando o estoque pelo painel, e e essa atualizacao que dispara o
 * alerta.</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AvisoEstoqueViewAdapter {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final int ENVIOS_LISTADOS = 15;
    private static final String PREFIXO_CRITICO = "critico_";
    private static final String PREFIXO_BAIXO = "baixo_";
    private static final String TELA = "redirect:/aviso-estoque";

    private final ConfigurarAvisoEstoqueUseCase configurarAviso;
    private final AvisarEquipeDoEstoqueUseCase avisarEquipe;
    private final ClassificarNivelEstoqueUseCase classificarNivel;
    private final IEstoqueRepositoryPort estoqueRepository;
    private final IAvisoEstoqueRepositoryPort avisoRepository;
    private final IUsuarioRepositoryPort usuarioRepository;
    private final EnvioDeEmailRouter roteadorDeEnvio;
    private final MessageSource messageSource;

    @GetMapping("/aviso-estoque")
    public String exibir(final Model model) {
        final List<LinhaDeMinimos> minimos = estoqueRepository.listarTodos().stream()
                .sorted(Comparator.comparing(IEstoqueRepositoryPort.RegistroEstoque::tipoSanguineo))
                .map(registro -> new LinhaDeMinimos(registro.tipoSanguineo(), registro.quantidadeBolsas(),
                        registro.minimoCritico(), registro.minimoBaixo(), classificarNivel.execute(registro).nivel()))
                .toList();

        model.addAttribute("configuracao", configurarAviso.consultar());
        model.addAttribute("destinatarios", usuarioRepository.emailsDosAdministradores());
        model.addAttribute("minimos", minimos);
        model.addAttribute("envios", avisoRepository.ultimosEnvios(ENVIOS_LISTADOS));
        model.addAttribute("envioReal", roteadorDeEnvio.isEntregando());

        return "aviso-estoque";
    }

    @PostMapping("/aviso-estoque")
    public String salvar(@RequestParam final int horaRelatorio,
                         final Principal autenticado,
                         final RedirectAttributes atributos) {
        return responder(atributos, () -> configurarAviso.salvar(horaRelatorio, autenticado.getName())
                ? "Hora do relatório diário salva." : "Nada foi alterado.");
    }

    @PostMapping("/aviso-estoque/minimos")
    public String salvarMinimos(@RequestParam final Map<String, String> campos, final RedirectAttributes atributos) {
        return responder(atributos, () -> {
            final List<MinimosDoTipo> minimos = Arrays.stream(TipoSanguineo.values())
                    .map(tipo -> new MinimosDoTipo(tipo, numero(campos.get(PREFIXO_CRITICO + tipo.name())),
                            numero(campos.get(PREFIXO_BAIXO + tipo.name()))))
                    .toList();
            final int alterados = configurarAviso.salvarMinimos(minimos);

            return alterados == 0 ? "Nada foi alterado." : "Níveis mínimos de %d tipo(s) atualizados.".formatted(alterados);
        });
    }

    @PostMapping("/aviso-estoque/teste")
    public String enviarTeste(final RedirectAttributes atributos) {
        return responder(atributos, () -> {
            final int destinatarios = avisarEquipe.enviarTeste(LocalDateTime.now(ConfiguracaoDisparoAutomatico.FUSO));

            return roteadorDeEnvio.isEntregando()
                    ? "Teste enviado a %d administrador(es).".formatted(destinatarios)
                    : "Teste registrado para %d administrador(es). O envio de e-mail está desligado, então nada saiu."
                            .formatted(destinatarios);
        });
    }

    private String responder(final RedirectAttributes atributos, final Acao acao) {
        try {
            atributos.addFlashAttribute("aviso", acao.executar());
        } catch (final NegocioException excecao) {
            atributos.addFlashAttribute("erro", messageSource.getMessage(excecao.getErro().getChaveMensagem(),
                    null, excecao.getErro().getChaveMensagem(), PT_BR));
        }

        return TELA;
    }

    private static int numero(final String texto) {
        try {
            return Integer.parseInt(texto == null ? "" : texto.trim());
        } catch (final NumberFormatException excecao) {
            throw new NegocioException(EstoqueErrorsMessage.MINIMOS_INVALIDOS);
        }
    }

    /** Acao de um formulario da tela, que devolve a mensagem de confirmacao. */
    @FunctionalInterface
    private interface Acao {
        String executar();
    }

    /**
     * Linha da tabela de minimos.
     *
     * @param tipo tipo sanguineo
     * @param quantidadeBolsas bolsas em estoque agora
     * @param minimoCritico abaixo desta quantidade o nivel e critico
     * @param minimoBaixo abaixo desta quantidade o nivel e baixo
     * @param nivel situacao atual
     */
    public record LinhaDeMinimos(TipoSanguineo tipo, int quantidadeBolsas, int minimoCritico, int minimoBaixo,
                                 NivelEstoque nivel) {
    }
}
