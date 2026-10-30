package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.domain.dto.Pagina;
import br.univille.sendhemosc.domain.dto.RegistroAuditoria;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Tela de auditoria: mostra o rastro que o sistema ja gravava de cada convocacao, de cada
 * mudanca no envio e de cada alteracao de conta. Restrita ao administrador nas regras de acesso.
 */
@Controller
@RequiredArgsConstructor
public class AuditoriaViewAdapter {

    private static final int POR_PAGINA = 30;

    /** Nome legivel de cada acao. Acao sem nome aqui aparece pelo proprio identificador. */
    private static final Map<String, String> NOMES = Map.ofEntries(
            Map.entry("CONVOCACAO_SELETIVA", "Convocação de selecionados"),
            Map.entry("CONVOCACAO_POR_TIPO", "Convocação por tipo sanguíneo"),
            Map.entry("CONVOCACAO_GERAL", "Convocação geral do painel"),
            Map.entry("CONVOCACAO_AUTOMATICA", "Convocação automática"),
            Map.entry("ENVIO_LIGADO", "Envio de e-mail ligado"),
            Map.entry("ENVIO_DESLIGADO", "Envio de e-mail desligado"),
            Map.entry("DISPARO_AUTOMATICO_LIGADO", "Disparo automático ligado"),
            Map.entry("DISPARO_AUTOMATICO_DESLIGADO", "Disparo automático desligado"),
            Map.entry("DISPARO_AUTOMATICO_ALTERADO", "Disparo automático alterado"),
            Map.entry("LIMITE_DE_CONTATO_LIBERADO", "Limite de contato liberado"),
            Map.entry("DOACAO_REGISTRADA", "Doação registrada"),
            Map.entry("DOADOR_EXCLUIDO", "Doador excluído"),
            Map.entry("DOADOR_DESATIVADO", "Doador desativado"),
            Map.entry("DOADOR_REATIVADO", "Doador reativado"),
            Map.entry("DADOS_DO_TITULAR_EXPORTADOS", "Dados do titular exportados"),
            Map.entry("USUARIO_CADASTRADO", "Cadastro de conta"),
            Map.entry("CONTA_CRIADA", "Conta criada"),
            Map.entry("CONTA_ALTERADA", "Conta alterada"),
            Map.entry("CONTA_EXCLUIDA", "Conta excluída"),
            Map.entry("SENHA_ALTERADA", "Senha alterada"),
            Map.entry("SENHA_REDEFINIDA", "Senha redefinida pelo administrador"),
            Map.entry("SENHA_REDEFINIDA_PELO_LINK", "Senha redefinida pelo link"),
            Map.entry("RECUPERACAO_DE_SENHA_PEDIDA", "Recuperação de senha pedida"),
            Map.entry("ENTRADA_RECUSADA", "Entrada recusada"),
            Map.entry("ENTRADA_BLOQUEADA", "Entrada bloqueada"));

    private final IAuditoriaPort auditoria;

    @GetMapping("/auditoria")
    public String listar(@RequestParam(required = false) final String acao,
                         @RequestParam(defaultValue = "1") final int pagina,
                         final Model model) {
        final Pagina<RegistroAuditoria> registros = auditoria.listar(acao, pagina, POR_PAGINA);

        final List<String> acoes = auditoria.acoesRegistradas();

        model.addAttribute("registros", registros);
        model.addAttribute("acao", acao);
        model.addAttribute("acoes", acoes);
        model.addAttribute("nomes", acoes.stream()
                .collect(Collectors.toMap(Function.identity(), AuditoriaViewAdapter::nomeDe)));
        model.addAttribute("urlPaginacao", ParaPaginacao.url(UriComponentsBuilder.fromPath("/auditoria")
                .queryParamIfPresent("acao", Optional.ofNullable(acao).filter(a -> !a.isBlank()))));

        return "auditoria";
    }

    private static String nomeDe(final String acao) {
        final String nome = NOMES.get(acao);
        if (nome != null) {
            return nome;
        }

        final String texto = acao.replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
