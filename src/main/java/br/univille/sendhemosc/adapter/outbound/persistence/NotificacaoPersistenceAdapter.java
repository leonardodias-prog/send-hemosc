package br.univille.sendhemosc.adapter.outbound.persistence;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.NotificacaoEntity;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.NotificacaoJpaRepository;
import br.univille.sendhemosc.domain.dto.ConversaoPorTipo;
import br.univille.sendhemosc.domain.enums.NivelEstoque;
import br.univille.sendhemosc.domain.enums.OrigemNotificacao;
import br.univille.sendhemosc.domain.enums.StatusNotificacao;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.port.outbound.INotificacaoRepositoryPort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementacao de persistencia da porta de notificacoes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificacaoPersistenceAdapter implements INotificacaoRepositoryPort {

    private final NotificacaoJpaRepository notificacaoRepository;

    @Override
    @Transactional
    public void registrar(final Long doadorId, final TipoSanguineo tipoSanguineo, final NivelEstoque nivel,
                          final OrigemNotificacao origem, final StatusNotificacao status,
                          final int tentativa, final String erro) {
        final LocalDateTime agora = LocalDateTime.now();

        notificacaoRepository.save(NotificacaoEntity.builder()
                .doadorId(doadorId)
                .tipoSanguineoAlvo(tipoSanguineo.getSigla())
                .nivelEstoque(nivel)
                .status(status)
                .origem(origem)
                .tentativa(tentativa)
                .enviadaEm(status == StatusNotificacao.ENVIADA ? agora : null)
                .erroMensagem(erro)
                .criadoEm(agora)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> buscarPendentesDeReenvio(final TipoSanguineo tipoSanguineo, final LocalDate anteriorA) {
        return notificacaoRepository.buscarPendentesDeReenvio(tipoSanguineo.getSigla(), anteriorA.atStartOfDay());
    }

    @Override
    @Transactional
    public int marcarComparecimento(final Long doadorId, final LocalDate dataDoacao) {
        final int fechadas = notificacaoRepository.marcarComparecimento(doadorId, dataDoacao);

        if (fechadas > 0) {
            log.info("[m=marcarComparecimento] {} convocacao(oes) do doador {} passaram a constar "
                    + "como atendidas em {}", fechadas, doadorId, dataDoacao);
        }

        return fechadas;
    }

    @Override
    @Transactional(readOnly = true)
    public int proximaTentativa(final Long doadorId, final TipoSanguineo tipoSanguineo) {
        return notificacaoRepository.maiorTentativa(doadorId, tipoSanguineo.getSigla()) + 1;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversaoPorTipo> contarConversaoPorTipo(final LocalDate inicio, final LocalDate fim) {
        final List<Object[]> linhas = notificacaoRepository.contarConversaoPorTipo(
                inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay());

        final Map<String, ConversaoPorTipo> porSigla = linhas.stream()
                .collect(Collectors.toMap(
                        linha -> (String) linha[0],
                        linha -> new ConversaoPorTipo(TipoSanguineo.doSigla((String) linha[0]),
                                (Long) linha[1], (Long) linha[2])));

        // Inclui os tipos sem convocacao no periodo, com zero, para o relatorio cobrir sempre
        // os oito tipos, e nao so os que tiveram movimento.
        return Arrays.stream(TipoSanguineo.values())
                .map(tipo -> porSigla.getOrDefault(tipo.getSigla(), new ConversaoPorTipo(tipo, 0, 0)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocalDateTime> listarEnviosNoPeriodo(final LocalDate inicio, final LocalDate fim) {
        return notificacaoRepository.listarEnvios(inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay());
    }
}
