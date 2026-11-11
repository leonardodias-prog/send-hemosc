package br.univille.sendhemosc.usecase.estoque;

import br.univille.sendhemosc.config.SendHemoscProperties;
import br.univille.sendhemosc.domain.dto.ConfiguracaoAvisoEstoque;
import br.univille.sendhemosc.domain.dto.ConfiguracaoDisparoAutomatico;
import br.univille.sendhemosc.domain.dto.EnvioAvisoEstoque;
import br.univille.sendhemosc.domain.dto.MensagemEmail;
import br.univille.sendhemosc.domain.dto.SituacaoEstoque;
import br.univille.sendhemosc.domain.enums.NivelEstoque;
import br.univille.sendhemosc.domain.enums.TipoAvisoEstoque;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.exception.EstoqueErrorsMessage;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IAvisoEstoqueRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.IEmailPort;
import br.univille.sendhemosc.domain.port.outbound.IEstoqueRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.IEstoqueRepositoryPort.RegistroEstoque;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import br.univille.sendhemosc.domain.util.MascaraDeEmail;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Avisa a equipe interna sobre o estoque, por e-mail: o relatorio do comeco do dia e o alerta
 * imediato quando um tipo cai de nivel.
 *
 * <p>E controle interno, e so os administradores recebem: os destinatarios sao as contas de
 * administrador ativas, lidas na hora do envio. Nao ha lista para manter nem como incluir outro
 * endereco; promover ou desativar uma conta e o que muda quem recebe.</p>
 *
 * <p>Nao ha como desligar: sem aviso, a equipe so descobre que um tipo acabou quando precisa
 * dele. O que se ajusta e a hora do relatorio diario.</p>
 *
 * <p>O alerta nao se repete: o ultimo nivel avisado de cada tipo fica gravado, e so sai outro
 * e-mail quando o tipo piora em relacao a ele. Voltar ao normal rearma o alerta.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AvisarEquipeDoEstoqueUseCase {

    private static final String TEMPLATE = "email/aviso-estoque";
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int MEIO_DIA = 12;
    private static final int FIM_DA_TARDE = 18;

    private final IEstoqueRepositoryPort estoqueRepository;
    private final IAvisoEstoqueRepositoryPort avisoRepository;
    private final IUsuarioRepositoryPort usuarioRepository;
    private final ListarSituacaoEstoqueUseCase listarSituacaoEstoque;
    private final ClassificarNivelEstoqueUseCase classificarNivel;
    private final IEmailPort emailPort;
    private final TemplateEngine templateEngine;
    private final SendHemoscProperties properties;

    /**
     * Envia o relatorio do dia, se ja passou do horario e se o de hoje ainda nao saiu.
     *
     * <p>O horario e "a partir de", e nao "exatamente": a hospedagem gratuita hiberna, e com hora
     * exata bastaria o servico estar dormindo naquele minuto para o relatorio do dia se perder.</p>
     *
     * @param agora momento da conferencia, no fuso de Sao Paulo
     * @return true quando o relatorio foi enviado nesta chamada
     */
    public boolean enviarRelatorioSeDevido(final LocalDateTime agora) {
        final ConfiguracaoAvisoEstoque configuracao = avisoRepository.buscar();

        if (agora.getHour() < configuracao.horaRelatorio()
                || agora.toLocalDate().equals(configuracao.ultimoRelatorioEm())) {
            return false;
        }

        final List<String> destinatarios = usuarioRepository.emailsDosAdministradores();

        // Sem assumir o dia: assim que houver um administrador ativo, o relatorio de hoje ainda sai.
        if (destinatarios.isEmpty() || !avisoRepository.assumirRelatorioDoDia(agora.toLocalDate())) {
            return false;
        }

        final List<SituacaoEstoque> situacoes = listarSituacaoEstoque.execute();
        final String assunto = "Estoque de sangue – " + DIA.format(agora);
        final String corpo = renderizar("Estoque de sangue em " + DIA.format(agora),
                saudacao(agora) + " Segue o estoque atual:", null, situacoes);

        enviar(TipoAvisoEstoque.RELATORIO_DIARIO, assunto, corpo, destinatarios, agora);
        return true;
    }

    /**
     * Confere se o tipo caiu de nivel desde o ultimo aviso e, se caiu, alerta a equipe.
     *
     * @param tipoSanguineo tipo cujo estoque ou cujos minimos acabaram de mudar
     * @return true quando um alerta foi enviado nesta chamada
     */
    public boolean avaliarAlerta(final TipoSanguineo tipoSanguineo) {
        final Optional<RegistroEstoque> registro = estoqueRepository.buscarPorTipo(tipoSanguineo);

        if (registro.isEmpty()) {
            return false;
        }

        final SituacaoEstoque situacao = classificarNivel.execute(registro.get());
        final NivelEstoque atual = situacao.nivel();
        final NivelEstoque avisado = estoqueRepository.nivelAvisado(tipoSanguineo).orElse(NivelEstoque.NORMAL);

        if (atual == avisado) {
            return false;
        }

        if (!atual.maisGraveQue(avisado)) {
            // Melhorou. Guardar o nivel novo rearma o alerta: uma nova queda volta a avisar.
            estoqueRepository.registrarNivelAvisado(tipoSanguineo, atual);
            return false;
        }

        final List<String> destinatarios = usuarioRepository.emailsDosAdministradores();

        // Sem administrador ativo, nada e registrado como avisado: quando houver um, a proxima
        // alteracao deste tipo avisa.
        if (destinatarios.isEmpty()) {
            return false;
        }

        // Registra antes de enviar: falha na entrega nao pode virar um alerta a cada bolsa retirada.
        estoqueRepository.registrarNivelAvisado(tipoSanguineo, atual);

        final LocalDateTime agora = LocalDateTime.now(ConfiguracaoDisparoAutomatico.FUSO);
        final String sigla = tipoSanguineo.getSigla();
        final String nivel = atual.getDescricao().toLowerCase();
        final String assunto = "Estoque %s: %s com %s".formatted(nivel, sigla, bolsas(situacao.quantidadeBolsas()));
        final int minimo = atual == NivelEstoque.CRITICO ? registro.get().minimoCritico() : registro.get().minimoBaixo();
        final Destaque destaque = new Destaque(sigla, bolsas(situacao.quantidadeBolsas()), atual.getDescricao(),
                atual.name().toLowerCase(), minimo);
        final String corpo = renderizar("Estoque " + nivel + ": " + sigla,
                "Um tipo sanguíneo acabou de cair de nível. Segue o estoque atual:", destaque,
                listarSituacaoEstoque.execute());

        enviar(TipoAvisoEstoque.ALERTA, assunto, corpo, destinatarios, agora);
        return true;
    }

    /**
     * Envia agora o relatorio do estoque aos administradores, a pedido de quem configura. Nao conta como
     * o relatorio do dia nem mexe no controle de alertas.
     *
     * @param agora momento do pedido, no fuso de Sao Paulo
     * @return para quantos enderecos o teste foi
     */
    public int enviarTeste(final LocalDateTime agora) {
        final List<String> destinatarios = usuarioRepository.emailsDosAdministradores();

        if (destinatarios.isEmpty()) {
            throw new NegocioException(EstoqueErrorsMessage.AVISO_SEM_DESTINATARIOS);
        }

        final String assunto = "[Teste] Estoque de sangue – " + DIA.format(agora);
        final String corpo = renderizar("Estoque de sangue em " + DIA.format(agora),
                "Este é um envio de teste do aviso de estoque. Segue o estoque atual:", null,
                listarSituacaoEstoque.execute());

        enviar(TipoAvisoEstoque.TESTE, assunto, corpo, destinatarios, agora);
        return destinatarios.size();
    }

    private void enviar(final TipoAvisoEstoque tipo, final String assunto, final String corpo,
                        final List<String> destinatarios, final LocalDateTime agora) {
        int falhas = 0;

        // Cada endereco e uma entrega propria: um endereco com problema nao cala o aviso dos demais.
        for (final String destinatario : destinatarios) {
            try {
                emailPort.enviar(new MensagemEmail(destinatario, assunto, corpo));
            } catch (final RuntimeException excecao) {
                falhas++;
                log.error("[m=enviar] Falha ao enviar o aviso de estoque a {}: {}",
                        MascaraDeEmail.mascarar(destinatario), MascaraDeEmail.mascararEmTexto(excecao.getMessage()));
            }
        }

        avisoRepository.registrarEnvio(new EnvioAvisoEstoque(tipo, assunto, destinatarios.size(), falhas, agora));
        log.info("[m=enviar] Aviso de estoque ({}) enviado a {} destinatario(s), {} falha(s)",
                tipo, destinatarios.size(), falhas);
    }

    private String renderizar(final String titulo, final String introducao, final Destaque destaque,
                              final List<SituacaoEstoque> situacoes) {
        final List<Linha> linhas = situacoes.stream()
                .map(situacao -> new Linha(situacao.tipoSanguineo().getSigla(), situacao.quantidadeBolsas(),
                        situacao.nivel().getDescricao(), situacao.nivel().name().toLowerCase(),
                        situacao.nivel().isExigeConvocacao()))
                .toList();
        final List<String> comAtencao = linhas.stream().filter(Linha::precisaAtencao).map(Linha::sigla).toList();

        final Context contexto = new Context();
        contexto.setVariable("titulo", titulo);
        contexto.setVariable("introducao", introducao);
        contexto.setVariable("destaque", destaque);
        contexto.setVariable("linhas", linhas);
        contexto.setVariable("tiposComAtencao", comAtencao.isEmpty() ? null : juntar(comAtencao));
        contexto.setVariable("linkPainel", properties.notificacao().urlBase() + "/");
        contexto.setVariable("logoUrl", properties.notificacao().urlDoLogo());

        return templateEngine.process(TEMPLATE, contexto);
    }

    private static String saudacao(final LocalDateTime agora) {
        if (agora.getHour() < MEIO_DIA) {
            return "Bom dia!";
        }

        return agora.getHour() < FIM_DA_TARDE ? "Boa tarde!" : "Boa noite!";
    }

    private static String bolsas(final int quantidade) {
        return quantidade == 1 ? "1 bolsa" : quantidade + " bolsas";
    }

    /** "O-", "O- e A-", "O-, A- e B-". */
    private static String juntar(final List<String> siglas) {
        if (siglas.size() == 1) {
            return siglas.getFirst();
        }

        return String.join(", ", siglas.subList(0, siglas.size() - 1)) + " e " + siglas.getLast();
    }

    /**
     * Linha da tabela do e-mail.
     *
     * @param sigla tipo sanguineo
     * @param bolsas quantidade em estoque
     * @param nivel situacao, por extenso
     * @param chave situacao em minusculas, para a cor
     * @param precisaAtencao se o tipo esta baixo ou critico
     */
    public record Linha(String sigla, int bolsas, String nivel, String chave, boolean precisaAtencao) {
    }

    /**
     * Tipo que motivou um alerta.
     *
     * @param sigla tipo sanguineo
     * @param bolsas quantidade em estoque, ja por extenso
     * @param nivel situacao, por extenso
     * @param chave situacao em minusculas, para a cor
     * @param minimo minimo que foi cruzado
     */
    public record Destaque(String sigla, String bolsas, String nivel, String chave, int minimo) {
    }
}
