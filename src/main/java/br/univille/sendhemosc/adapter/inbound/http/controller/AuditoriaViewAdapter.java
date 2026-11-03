package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.domain.dto.Pagina;
import br.univille.sendhemosc.domain.dto.RegistroAuditoria;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Tela de auditoria: mostra o rastro que o sistema ja gravava de cada convocacao, de cada
 * mudanca no envio e de cada alteracao de conta. Restrita ao administrador nas regras de acesso.
 *
 * <p>Os registros aparecem agrupados por dia, com a categoria de cada acao (que define icone e
 * cor), e podem ser filtrados por categoria e por texto (pessoa, e-mail ou detalhe).</p>
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

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", PT_BR);

    /** Categorias da tela, na ordem dos filtros. Acao fora de todas so aparece em "Tudo". */
    private static final Map<String, Categoria> CATEGORIAS = new LinkedHashMap<>();

    static {
        CATEGORIAS.put("convocacoes", new Categoria("convocacoes", "Convocações", Set.of(
                "CONVOCACAO_SELETIVA", "CONVOCACAO_AUTOMATICA", "CONVOCACAO_POR_TIPO", "CONVOCACAO_GERAL")));
        CATEGORIAS.put("envio", new Categoria("envio", "Envio de e-mail", Set.of(
                "ENVIO_LIGADO", "ENVIO_DESLIGADO", "DISPARO_AUTOMATICO_LIGADO", "DISPARO_AUTOMATICO_DESLIGADO",
                "DISPARO_AUTOMATICO_ALTERADO")));
        CATEGORIAS.put("doadores", new Categoria("doadores", "Doadores", Set.of(
                "DOACAO_REGISTRADA", "DOADOR_EXCLUIDO", "DOADOR_DESATIVADO", "DOADOR_REATIVADO",
                "DADOS_DO_TITULAR_EXPORTADOS", "LIMITE_DE_CONTATO_LIBERADO")));
        CATEGORIAS.put("contas", new Categoria("contas", "Contas e senhas", Set.of(
                "USUARIO_CADASTRADO", "CONTA_CRIADA", "CONTA_ALTERADA", "CONTA_EXCLUIDA", "SENHA_ALTERADA",
                "SENHA_REDEFINIDA", "SENHA_REDEFINIDA_PELO_LINK", "RECUPERACAO_DE_SENHA_PEDIDA")));
        CATEGORIAS.put("seguranca", new Categoria("seguranca", "Tentativas de entrada", Set.of(
                "ENTRADA_RECUSADA", "ENTRADA_BLOQUEADA")));
    }

    private final IAuditoriaPort auditoria;

    @GetMapping("/auditoria")
    public String listar(@RequestParam(required = false) final String categoria,
                         @RequestParam(required = false) final String acao,
                         @RequestParam(required = false) final String busca,
                         @RequestParam(defaultValue = "1") final int pagina,
                         final Model model) {
        final Categoria escolhida = categoria == null ? null : CATEGORIAS.get(categoria);
        final Set<String> acoes = acao != null && !acao.isBlank()
                ? Set.of(acao)
                : escolhida == null ? null : escolhida.acoes();
        final Pagina<RegistroAuditoria> registros = auditoria.listar(acoes, busca, pagina, POR_PAGINA);

        model.addAttribute("registros", registros);
        model.addAttribute("dias", agruparPorDia(registros.itens()));
        model.addAttribute("categorias", CATEGORIAS.values());
        model.addAttribute("categoria", escolhida == null ? "" : escolhida.chave());
        model.addAttribute("busca", busca);
        model.addAttribute("urlPaginacao", ParaPaginacao.url(UriComponentsBuilder.fromPath("/auditoria")
                .queryParamIfPresent("categoria", Optional.ofNullable(escolhida).map(Categoria::chave))
                .queryParamIfPresent("acao", Optional.ofNullable(acao).filter(a -> !a.isBlank()))
                .queryParamIfPresent("busca", Optional.ofNullable(busca).filter(b -> !b.isBlank()))));

        return "auditoria";
    }

    private List<Dia> agruparPorDia(final List<RegistroAuditoria> registros) {
        final LocalDate hoje = LocalDate.now();
        final List<Dia> dias = new ArrayList<>();

        for (final RegistroAuditoria registro : registros) {
            final LocalDate data = registro.ocorridoEm().toLocalDate();
            if (dias.isEmpty() || !dias.getLast().data().equals(data)) {
                dias.add(new Dia(data, rotulo(data, hoje), new ArrayList<>()));
            }
            dias.getLast().linhas().add(new Linha(registro, nomeDe(registro.acao()), categoriaDe(registro.acao())));
        }

        return dias;
    }

    private static String rotulo(final LocalDate data, final LocalDate hoje) {
        if (data.equals(hoje)) {
            return "Hoje";
        }
        if (data.equals(hoje.minusDays(1))) {
            return "Ontem";
        }
        final String texto = data.format(DIA);
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    private static String categoriaDe(final String acao) {
        return CATEGORIAS.values().stream()
                .filter(categoria -> categoria.acoes().contains(acao))
                .map(Categoria::chave)
                .findFirst()
                .orElse("outros");
    }

    private static String nomeDe(final String acao) {
        final String nome = NOMES.get(acao);
        if (nome != null) {
            return nome;
        }

        final String texto = acao.replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    /**
     * Grupo de acoes que a tela filtra junto.
     *
     * @param chave identificador no endereco
     * @param nome rotulo do filtro
     * @param acoes acoes da categoria
     */
    public record Categoria(String chave, String nome, Set<String> acoes) {
    }

    /**
     * Os registros de um dia.
     *
     * @param data dia
     * @param rotulo "Hoje", "Ontem" ou a data por extenso
     * @param linhas registros do dia, do mais novo ao mais antigo
     */
    public record Dia(LocalDate data, String rotulo, List<Linha> linhas) {
    }

    /**
     * Um registro como a tela mostra.
     *
     * @param registro o registro gravado
     * @param nome nome legivel da acao
     * @param categoria chave da categoria, que define icone e cor
     */
    public record Linha(RegistroAuditoria registro, String nome, String categoria) {
    }
}
