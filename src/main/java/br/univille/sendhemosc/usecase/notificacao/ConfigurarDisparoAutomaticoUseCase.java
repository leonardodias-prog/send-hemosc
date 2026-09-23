package br.univille.sendhemosc.usecase.notificacao;

import br.univille.sendhemosc.domain.dto.AjusteDisparoAutomatico;
import br.univille.sendhemosc.domain.dto.ConfiguracaoDisparoAutomatico;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.exception.DisparoErrorsMessage;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import br.univille.sendhemosc.domain.port.outbound.IDisparoAutomaticoRepositoryPort;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Consulta e altera a configuracao do disparo automatico.
 *
 * <p>Toda alteracao fica em auditoria com o nome de quem a fez, e ligar e desligar ganham acoes
 * proprias, por serem o que se procura primeiro quando um e-mail inesperado chega a alguem.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigurarDisparoAutomaticoUseCase {

    private static final int LIMITE_MAXIMO_POR_RODADA = 1000;
    private static final int INTERVALO_MAXIMO_DIAS = 30;
    private static final int ULTIMA_HORA_DO_DIA = 24;

    private final IDisparoAutomaticoRepositoryPort disparoRepository;
    private final IAuditoriaPort auditoria;

    public ConfiguracaoDisparoAutomatico consultar() {
        return disparoRepository.buscar();
    }

    /**
     * Valida e grava o ajuste.
     *
     * @param ajuste campos alterados na tela
     * @param autor quem alterou
     * @return o que mudou, para a tela dizer o que aconteceu
     */
    public Alteracao salvar(final AjusteDisparoAutomatico ajuste, final String autor) {
        validar(ajuste);

        final ConfiguracaoDisparoAutomatico vigente = disparoRepository.buscar();
        if (AjusteDisparoAutomatico.de(vigente).equals(ajuste)) {
            return Alteracao.NENHUMA;
        }

        disparoRepository.salvar(ajuste, autor, LocalDateTime.now(ConfiguracaoDisparoAutomatico.FUSO));

        final Alteracao alteracao = classificar(vigente, ajuste);
        auditoria.registrar(alteracao.acaoDeAuditoria, descrever(ajuste));

        return alteracao;
    }

    private void validar(final AjusteDisparoAutomatico ajuste) {
        if (ajuste.limitePorRodada() < 1 || ajuste.limitePorRodada() > LIMITE_MAXIMO_POR_RODADA) {
            throw new NegocioException(DisparoErrorsMessage.LIMITE_INVALIDO);
        }
        if (ajuste.horaInicio() < 0 || ajuste.horaFim() > ULTIMA_HORA_DO_DIA
                || ajuste.horaInicio() >= ajuste.horaFim()) {
            throw new NegocioException(DisparoErrorsMessage.JANELA_INVALIDA);
        }
        if (ajuste.intervaloDias() < 1 || ajuste.intervaloDias() > INTERVALO_MAXIMO_DIAS) {
            throw new NegocioException(DisparoErrorsMessage.INTERVALO_INVALIDO);
        }
        // Desligado sem tipo marcado e valido; ligado sem tipo seria uma rodada que nunca convoca.
        if (ajuste.ativo() && ajuste.tiposSanguineos().isEmpty()) {
            throw new NegocioException(DisparoErrorsMessage.SEM_TIPOS);
        }
    }

    private Alteracao classificar(final ConfiguracaoDisparoAutomatico vigente, final AjusteDisparoAutomatico ajuste) {
        if (vigente.ativo() == ajuste.ativo()) {
            return Alteracao.ALTERADO;
        }

        return ajuste.ativo() ? Alteracao.LIGADO : Alteracao.DESLIGADO;
    }

    private String descrever(final AjusteDisparoAutomatico ajuste) {
        final EnumSet<TipoSanguineo> tipos = EnumSet.noneOf(TipoSanguineo.class);
        tipos.addAll(ajuste.tiposSanguineos());

        return "%s; tipos %s; ate %d por rodada; das %dh as %dh; a cada %d dia(s)".formatted(
                ajuste.ativo() ? "ligado" : "desligado",
                tipos.isEmpty() ? "nenhum" : tipos.stream().map(TipoSanguineo::getSigla)
                        .collect(Collectors.joining(", ")),
                ajuste.limitePorRodada(), ajuste.horaInicio(), ajuste.horaFim(), ajuste.intervaloDias());
    }

    /**
     * Efeito de um ajuste sobre a configuracao vigente.
     */
    public enum Alteracao {

        NENHUMA(null),
        LIGADO("DISPARO_AUTOMATICO_LIGADO"),
        DESLIGADO("DISPARO_AUTOMATICO_DESLIGADO"),
        ALTERADO("DISPARO_AUTOMATICO_ALTERADO");

        private final String acaoDeAuditoria;

        Alteracao(final String acaoDeAuditoria) {
            this.acaoDeAuditoria = acaoDeAuditoria;
        }
    }
}
