package br.univille.sendhemosc.usecase.notificacao;

import br.univille.sendhemosc.domain.dto.CandidatoConvocacao;
import br.univille.sendhemosc.domain.dto.ResultadoConvocacao;
import br.univille.sendhemosc.domain.dto.SituacaoEstoque;
import br.univille.sendhemosc.domain.enums.OrigemNotificacao;
import br.univille.sendhemosc.domain.enums.StatusNotificacao;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.exception.EstoqueErrorsMessage;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.IEmailPort;
import br.univille.sendhemosc.domain.port.outbound.IEstoqueRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.INotificacaoRepositoryPort;
import br.univille.sendhemosc.usecase.doador.CalcularAptidaoUseCase;
import br.univille.sendhemosc.usecase.estoque.ClassificarNivelEstoqueUseCase;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Convoca uma selecao explicita de doadores, um ou varios.
 *
 * <p>A convocacao por tipo sanguineo atende o caso principal, mas nem toda situacao e essa.
 * Um doador raro que a equipe conhece, um grupo que ja se dispos, um reforco pontual: nesses
 * casos disparar para a base inteira e desproporcional. Aqui quem escolhe e quem opera.</p>
 *
 * <p>Escolher nao dispensa a regra: quem nao esta apto, nao autorizou contato ou atingiu os
 * limites de contato continua fora, mesmo selecionado na tela. Selecao decide a quem oferecer,
 * nao a quem a regra se aplica.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConvocarSelecionadosUseCase {

    private final IDoadorRepositoryPort doadorRepository;
    private final IEstoqueRepositoryPort estoqueRepository;
    private final INotificacaoRepositoryPort notificacaoRepository;
    private final IEmailPort emailPort;
    private final IAuditoriaPort auditoria;
    private final ClassificarNivelEstoqueUseCase classificarNivel;
    private final CalcularAptidaoUseCase calcularAptidao;
    private final AvaliarLimiteDeContatoUseCase avaliarLimiteDeContato;
    private final RenderizarConvocacaoUseCase renderizarConvocacao;

    /**
     * Convoca os doadores escolhidos.
     *
     * @param identificadores doadores selecionados na tela
     * @return resumo da rodada
     */
    public ResultadoConvocacao execute(final Set<Long> identificadores) {
        final LocalDate hoje = LocalDate.now();

        final List<CandidatoConvocacao> selecionados =
                doadorRepository.buscarPorIdentificadores(identificadores, hoje);

        final List<CandidatoConvocacao> aptos = selecionados.stream()
                .filter(CandidatoConvocacao::aceitaContato)
                .filter(candidato -> estaApto(candidato, hoje))
                .toList();

        final List<CandidatoConvocacao> liberados = aptos.stream()
                .filter(candidato -> dentroDoLimite(candidato, hoje))
                .toList();
        final int retidos = aptos.size() - liberados.size();

        log.info("[m=execute] Convocacao seletiva: {} escolhidos, {} aptos e com consentimento, "
                + "{} retidos pelo limite de contato", selecionados.size(), aptos.size(), retidos);

        int enviados = 0;
        int falhas = 0;

        for (final CandidatoConvocacao doador : liberados) {
            final TipoSanguineo tipo = doador.tipoSanguineo();
            final SituacaoEstoque situacao = situacaoDoTipo(tipo);
            final int tentativa = notificacaoRepository.proximaTentativa(doador.id(), tipo);

            try {
                emailPort.enviar(renderizarConvocacao.execute(doador, situacao));
                notificacaoRepository.registrar(doador.id(), tipo, situacao.nivel(),
                        OrigemNotificacao.MANUAL, StatusNotificacao.ENVIADA, tentativa, null);
                enviados++;
            } catch (final RuntimeException excecao) {
                log.warn("[m=execute] Falha ao convocar doador {}: {}", doador.id(), excecao.getMessage());
                notificacaoRepository.registrar(doador.id(), tipo, situacao.nivel(),
                        OrigemNotificacao.MANUAL, StatusNotificacao.FALHA, tentativa, excecao.getMessage());
                falhas++;
            }
        }

        auditoria.registrar("CONVOCACAO_SELETIVA",
                "%d selecionado(s), %d apto(s), %d retido(s) pelo limite de contato, %d enviado(s), %d falha(s)"
                        .formatted(identificadores.size(), aptos.size(), retidos, enviados, falhas));

        // O tipo sanguineo nao se aplica a uma selecao mista; o resumo reporta apenas os totais.
        return new ResultadoConvocacao(null, null, liberados.size(), enviados, falhas, retidos, false);
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

    private SituacaoEstoque situacaoDoTipo(final TipoSanguineo tipo) {
        final var registro = estoqueRepository.buscarPorTipo(tipo)
                .orElseThrow(() -> new NegocioException(EstoqueErrorsMessage.NAO_ENCONTRADO));

        return classificarNivel.execute(registro.tipoSanguineo(),
                registro.quantidadeBolsas(), registro.capacidadeAlvo());
    }
}
