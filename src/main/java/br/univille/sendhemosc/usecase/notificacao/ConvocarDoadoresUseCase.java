package br.univille.sendhemosc.usecase.notificacao;

import br.univille.sendhemosc.domain.dto.CandidatoConvocacao;
import br.univille.sendhemosc.domain.dto.ResultadoConvocacao;
import br.univille.sendhemosc.domain.dto.SelecaoConvocacao;
import br.univille.sendhemosc.domain.dto.SituacaoEstoque;
import br.univille.sendhemosc.domain.enums.OrigemNotificacao;
import br.univille.sendhemosc.domain.enums.StatusNotificacao;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.exception.EstoqueErrorsMessage;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.IEmailPort;
import br.univille.sendhemosc.domain.port.outbound.IEstoqueRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.INotificacaoRepositoryPort;
import br.univille.sendhemosc.usecase.doador.CalcularAptidaoUseCase;
import br.univille.sendhemosc.usecase.estoque.ClassificarNivelEstoqueUseCase;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Cruza a situacao do estoque com a aptidao individual dos doadores e dispara a convocacao
 * segmentada. E a operacao central do projeto: substitui a campanha generica pelo contato
 * dirigido a quem pode ajudar agora.
 *
 * <p>Selecao e envio sao etapas separadas. A convocacao manual usa as duas em sequencia; o
 * disparo automatico seleciona varios tipos antes de enviar, para respeitar o limite da rodada
 * e nao convidar duas vezes quem e compativel com mais de um tipo em falta.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConvocarDoadoresUseCase {

    /**
     * Quem nunca foi convocado vem primeiro, depois quem foi ha mais tempo. So faz diferenca
     * quando ha limite de envios, e ai decide quem fica para a proxima rodada: nao devem ser
     * sempre as mesmas pessoas.
     */
    private static final Comparator<CandidatoConvocacao> PRIORIDADE = Comparator
            .comparing(CandidatoConvocacao::ultimaConvocacao, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(CandidatoConvocacao::id);

    private final IEstoqueRepositoryPort estoqueRepository;
    private final IDoadorRepositoryPort doadorRepository;
    private final INotificacaoRepositoryPort notificacaoRepository;
    private final IEmailPort emailPort;
    private final ClassificarNivelEstoqueUseCase classificarNivel;
    private final CalcularAptidaoUseCase calcularAptidao;
    private final AvaliarLimiteDeContatoUseCase avaliarLimiteDeContato;
    private final RenderizarConvocacaoUseCase renderizarConvocacao;

    /**
     * Interrompe a rodada apos esta quantidade de falhas seguidas. Falha em sequencia indica
     * problema de infraestrutura, e nao do destinatario: insistir so faz a requisicao pendurar,
     * ja que cada tentativa espera o tempo limite de conexao antes de desistir.
     */
    @Value("${sendhemosc.email.max-falhas-consecutivas:5}")
    private int maxFalhasConsecutivas;

    /**
     * Convoca doadores compativeis com o tipo sanguineo informado.
     *
     * @param tipoSanguineo tipo em falta
     * @param origem se o disparo foi automatico ou manual
     * @param ignorarNivel quando true, convoca mesmo com o estoque em nivel NORMAL
     * @return resumo da rodada de convocacao
     */
    public ResultadoConvocacao execute(final TipoSanguineo tipoSanguineo, final OrigemNotificacao origem,
                                       final boolean ignorarNivel) {
        final SituacaoEstoque situacao = situacaoDoTipo(tipoSanguineo);

        if (!ignorarNivel && !situacao.nivel().isExigeConvocacao()) {
            log.info("[m=execute] Estoque {} em nivel {}, nenhuma convocacao disparada",
                    tipoSanguineo.getSigla(), situacao.nivel());
            return new ResultadoConvocacao(tipoSanguineo, situacao.nivel(), 0, 0, 0, 0, false);
        }

        final SelecaoConvocacao selecao = selecionar(situacao, LocalDate.now());
        return enviar(selecao, selecao.elegiveis(), origem);
    }

    /**
     * Quem pode ser convocado para o tipo agora: compativel, apto e dentro dos limites de contato,
     * em ordem de prioridade. Nao envia nada.
     *
     * @param situacao situacao do estoque do tipo em falta
     * @param referencia data da convocacao
     * @return elegiveis em ordem de prioridade, e os aptos que os limites de contato retiveram
     */
    public SelecaoConvocacao selecionar(final SituacaoEstoque situacao, final LocalDate referencia) {
        final TipoSanguineo tipoSanguineo = situacao.tipoSanguineo();

        final Map<Boolean, List<CandidatoConvocacao>> aptosPorLimite = doadorRepository
                .buscarCandidatos(tipoSanguineo.siglasDoadoresCompativeis(), referencia).stream()
                .filter(candidato -> estaApto(candidato, referencia))
                .collect(Collectors.partitioningBy(candidato -> dentroDoLimite(candidato, referencia)));

        final List<CandidatoConvocacao> elegiveis = aptosPorLimite.get(true).stream()
                .sorted(PRIORIDADE)
                .toList();
        final List<CandidatoConvocacao> retidos = aptosPorLimite.get(false);

        log.info("[m=selecionar] Tipo {} em nivel {} ({}%): {} elegiveis, {} retidos pelo limite de contato",
                tipoSanguineo.getSigla(), situacao.nivel(), situacao.percentualOcupacao(),
                elegiveis.size(), retidos.size());

        return new SelecaoConvocacao(situacao, elegiveis, retidos);
    }

    /**
     * Envia a convocacao aos destinatarios e registra cada tentativa.
     *
     * @param selecao selecao de onde os destinatarios saem
     * @param destinatarios quem recebe nesta rodada; a selecao inteira ou parte dela
     * @param origem se o disparo foi automatico ou manual
     * @return resumo do envio
     */
    public ResultadoConvocacao enviar(final SelecaoConvocacao selecao, final List<CandidatoConvocacao> destinatarios,
                                      final OrigemNotificacao origem) {
        final SituacaoEstoque situacao = selecao.situacao();
        final TipoSanguineo tipoSanguineo = situacao.tipoSanguineo();
        int enviados = 0;
        int falhas = 0;
        int falhasSeguidas = 0;
        boolean interrompida = false;

        for (final CandidatoConvocacao doador : destinatarios) {
            if (falhasSeguidas >= maxFalhasConsecutivas) {
                log.error("[m=enviar] Rodada interrompida apos {} falhas seguidas. "
                        + "Verifique a conectividade com o servidor de e-mail", falhasSeguidas);
                interrompida = true;
                break;
            }

            final int tentativa = notificacaoRepository.proximaTentativa(doador.id(), tipoSanguineo);
            try {
                emailPort.enviar(renderizarConvocacao.execute(doador, situacao));
                notificacaoRepository.registrar(doador.id(), tipoSanguineo, situacao.nivel(), origem,
                        StatusNotificacao.ENVIADA, tentativa, null);
                enviados++;
                falhasSeguidas = 0;
            } catch (final RuntimeException excecao) {
                log.warn("[m=enviar] Falha ao convocar doador {}: {}", doador.id(), excecao.getMessage());
                notificacaoRepository.registrar(doador.id(), tipoSanguineo, situacao.nivel(), origem,
                        StatusNotificacao.FALHA, tentativa, excecao.getMessage());
                falhas++;
                falhasSeguidas++;
            }
        }

        return new ResultadoConvocacao(tipoSanguineo, situacao.nivel(), selecao.elegiveis().size(),
                enviados, falhas, selecao.retidos().size(), interrompida);
    }

    private SituacaoEstoque situacaoDoTipo(final TipoSanguineo tipoSanguineo) {
        final var registro = estoqueRepository.buscarPorTipo(tipoSanguineo)
                .orElseThrow(() -> new NegocioException(EstoqueErrorsMessage.NAO_ENCONTRADO));

        return classificarNivel.execute(
                registro.tipoSanguineo(), registro.quantidadeBolsas(), registro.capacidadeAlvo());
    }

    private boolean estaApto(final CandidatoConvocacao candidato, final LocalDate referencia) {
        return calcularAptidao.execute(new CalcularAptidaoUseCase.DadosAptidao(
                candidato.sexo(),
                candidato.dataNascimento(),
                candidato.pesoKg(),
                candidato.ultimaDoacao(),
                candidato.doacoesUltimosDozeMeses(),
                referencia)).apto();
    }

    private boolean dentroDoLimite(final CandidatoConvocacao candidato, final LocalDate referencia) {
        return avaliarLimiteDeContato.execute(candidato.convocacoesSemResposta(),
                candidato.ultimaConvocacao(), referencia).liberado();
    }
}
