package br.univille.sendhemosc.usecase.notificacao;

import br.univille.sendhemosc.domain.dto.CandidatoConvocacao;
import br.univille.sendhemosc.domain.dto.ResultadoConvocacao;
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
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Cruza a situacao do estoque com a aptidao individual dos doadores e dispara a convocacao
 * segmentada. E a operacao central do projeto: substitui a campanha generica pelo contato
 * dirigido a quem pode ajudar agora.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConvocarDoadoresUseCase {

    private final IEstoqueRepositoryPort estoqueRepository;
    private final IDoadorRepositoryPort doadorRepository;
    private final INotificacaoRepositoryPort notificacaoRepository;
    private final IEmailPort emailPort;
    private final ClassificarNivelEstoqueUseCase classificarNivel;
    private final CalcularAptidaoUseCase calcularAptidao;
    private final RenderizarConvocacaoUseCase renderizarConvocacao;

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
        final var registro = estoqueRepository.buscarPorTipo(tipoSanguineo)
                .orElseThrow(() -> new NegocioException(EstoqueErrorsMessage.NAO_ENCONTRADO));

        final SituacaoEstoque situacao = classificarNivel.execute(
                registro.tipoSanguineo(), registro.quantidadeBolsas(), registro.capacidadeAlvo());

        if (!ignorarNivel && !situacao.nivel().isExigeConvocacao()) {
            log.info("[m=execute] Estoque {} em nivel {}, nenhuma convocacao disparada",
                    tipoSanguineo.getSigla(), situacao.nivel());
            return new ResultadoConvocacao(tipoSanguineo, situacao.nivel(), 0, 0, 0);
        }

        final LocalDate hoje = LocalDate.now();
        final List<CandidatoConvocacao> aptos = doadorRepository
                .buscarCandidatos(tipoSanguineo.siglasDoadoresCompativeis(), hoje).stream()
                .filter(candidato -> estaApto(candidato, hoje))
                .toList();

        log.info("[m=execute] Tipo {} em nivel {} ({}%): {} doadores aptos",
                tipoSanguineo.getSigla(), situacao.nivel(), situacao.percentualOcupacao(), aptos.size());

        return disparar(aptos, situacao, origem);
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

    private ResultadoConvocacao disparar(final List<CandidatoConvocacao> aptos, final SituacaoEstoque situacao,
                                         final OrigemNotificacao origem) {
        final TipoSanguineo tipoSanguineo = situacao.tipoSanguineo();
        int enviados = 0;
        int falhas = 0;

        for (final CandidatoConvocacao doador : aptos) {
            final int tentativa = notificacaoRepository.proximaTentativa(doador.id(), tipoSanguineo);
            try {
                emailPort.enviar(renderizarConvocacao.execute(doador, situacao));
                notificacaoRepository.registrar(doador.id(), tipoSanguineo, situacao.nivel(), origem,
                        StatusNotificacao.ENVIADA, tentativa, null);
                enviados++;
            } catch (final RuntimeException excecao) {
                log.warn("[m=disparar] Falha ao convocar doador {}: {}", doador.id(), excecao.getMessage());
                notificacaoRepository.registrar(doador.id(), tipoSanguineo, situacao.nivel(), origem,
                        StatusNotificacao.FALHA, tentativa, excecao.getMessage());
                falhas++;
            }
        }

        return new ResultadoConvocacao(tipoSanguineo, situacao.nivel(), aptos.size(), enviados, falhas);
    }
}
