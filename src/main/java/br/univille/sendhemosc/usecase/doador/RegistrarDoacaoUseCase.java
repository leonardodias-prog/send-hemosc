package br.univille.sendhemosc.usecase.doador;

import br.univille.sendhemosc.domain.dto.CandidatoConvocacao;
import br.univille.sendhemosc.domain.dto.NovaDoacao;
import br.univille.sendhemosc.domain.dto.ResultadoRegistroDoacao;
import br.univille.sendhemosc.domain.exception.DoacaoErrorsMessage;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import br.univille.sendhemosc.domain.port.outbound.IDoacaoRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.INotificacaoRepositoryPort;
import java.time.LocalDate;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra que uma doacao aconteceu e fecha as convocacoes que o doador tinha em aberto.
 *
 * <p>E o que fecha o ciclo do sistema. Ate aqui o sistema convocava e nunca sabia quem tinha
 * atendido: a tabela de doacoes so recebia linhas da massa ficticia, o calculo de aptidao lia
 * uma ultima doacao que nunca mudava, e quem doou continuava sendo convocado a cada rodada.</p>
 *
 * <p>Uma decisao importante: o registro <strong>nao</strong> e recusado quando a pessoa constava
 * como inapta. Quem avalia aptidao clinica e a triagem do servico de hemoterapia, nao este
 * sistema; se a pessoa doou, o fato aconteceu e precisa ser registrado. Recusar faria o
 * historico mentir. O caso fica sinalizado no retorno e no log, porque divergencia entre o que
 * o sistema calculou e o que a triagem decidiu e informacao util: ou a regra configurada esta
 * errada, ou houve avaliacao clinica que a sobrepos.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrarDoacaoUseCase {

    private final IDoacaoRepositoryPort doacaoRepository;
    private final IDoadorRepositoryPort doadorRepository;
    private final INotificacaoRepositoryPort notificacaoRepository;
    private final IAuditoriaPort auditoria;
    private final CalcularAptidaoUseCase calcularAptidao;

    /**
     * Executa o registro.
     *
     * @param doacao dados informados
     * @return desfecho, com quantas convocacoes foram fechadas e quando a pessoa volta a poder doar
     */
    @Transactional
    public ResultadoRegistroDoacao execute(final NovaDoacao doacao) {
        if (doacao.dataDoacao().isAfter(LocalDate.now())) {
            throw new NegocioException(DoacaoErrorsMessage.DATA_FUTURA);
        }

        final CandidatoConvocacao doador = doadorRepository
                .buscarPorIdentificadores(Set.of(doacao.doadorId()), doacao.dataDoacao()).stream()
                .findFirst()
                .orElseThrow(() -> new NegocioException(DoacaoErrorsMessage.DOADOR_NAO_ENCONTRADO));

        if (doacaoRepository.existeNoDia(doacao.doadorId(), doacao.dataDoacao())) {
            throw new NegocioException(DoacaoErrorsMessage.JA_REGISTRADA_NO_DIA);
        }

        final boolean estavaApto = avaliarAptidao(doador, doacao.dataDoacao());
        if (!estavaApto) {
            log.warn("[m=execute] Doacao registrada para o doador {}, que constava como inapto em {}. "
                    + "Verifique se a regra configurada corresponde ao criterio da triagem",
                    doacao.doadorId(), doacao.dataDoacao());
        }

        final Long doacaoId = doacaoRepository.registrar(doacao);
        final int fechadas = notificacaoRepository.marcarComparecimento(doacao.doadorId(), doacao.dataDoacao());

        auditoria.registrar("DOACAO_REGISTRADA", "%s doou em %s; %d convocacao(oes) fechada(s)%s"
                .formatted(doador.nome(), doacao.dataDoacao(), fechadas,
                        estavaApto ? "" : "; constava como inapto"));

        return new ResultadoRegistroDoacao(doacaoId, doador.nome(), fechadas,
                proximaDataApta(doador, doacao.dataDoacao()), estavaApto);
    }

    private boolean avaliarAptidao(final CandidatoConvocacao doador, final LocalDate referencia) {
        return calcularAptidao.execute(dados(doador, doador.ultimaDoacao(),
                doador.doacoesUltimosDozeMeses(), referencia)).apto();
    }

    /**
     * Calcula quando a pessoa volta a poder doar, considerando a doacao que acabou de ser
     * registrada como a mais recente.
     *
     * @param doador quem doou
     * @param dataDoacao data da doacao registrada
     * @return data estimada da proxima doacao possivel
     */
    private LocalDate proximaDataApta(final CandidatoConvocacao doador, final LocalDate dataDoacao) {
        return calcularAptidao.execute(dados(doador, dataDoacao,
                doador.doacoesUltimosDozeMeses() + 1, dataDoacao)).proximaDataApta();
    }

    private CalcularAptidaoUseCase.DadosAptidao dados(final CandidatoConvocacao doador,
                                                      final LocalDate ultimaDoacao,
                                                      final long doacoesNaJanela,
                                                      final LocalDate referencia) {
        return new CalcularAptidaoUseCase.DadosAptidao(
                doador.sexo(), doador.dataNascimento(), doador.pesoKg(),
                ultimaDoacao, doacoesNaJanela, referencia);
    }
}
