package br.univille.sendhemosc.adapter.outbound.persistence;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.NotificacaoEntity;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.NotificacaoJpaRepository;
import br.univille.sendhemosc.domain.enums.NivelEstoque;
import br.univille.sendhemosc.domain.enums.OrigemNotificacao;
import br.univille.sendhemosc.domain.enums.StatusNotificacao;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.port.outbound.INotificacaoRepositoryPort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
}
