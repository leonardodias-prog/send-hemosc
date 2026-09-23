package br.univille.sendhemosc.adapter.inbound.scheduler;

import br.univille.sendhemosc.domain.dto.ConfiguracaoDisparoAutomatico;
import br.univille.sendhemosc.usecase.notificacao.ExecutarDisparoAutomaticoUseCase;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Adapter de entrada por agendamento: confere de tempos em tempos se a rodada automatica e devida.
 *
 * <p>Se ha rodada, para quais tipos, quantos por vez e em que horario, quem decide e a configuracao
 * feita na tela. Aqui existe apenas o relogio.</p>
 *
 * <p>Conferir a cada poucos minutos, em vez de marcar uma hora fixa, e o que faz a rodada acontecer
 * na hospedagem gratuita. O servico hiberna sem acesso e so volta quando alguem abre o sistema; com
 * hora fixa, bastaria ele estar dormindo naquele minuto para a rodada do dia se perder. Conferindo,
 * ela acontece na primeira oportunidade dentro da janela.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConvocacaoScheduler {

    private final ExecutarDisparoAutomaticoUseCase executarDisparoAutomatico;

    @Scheduled(fixedDelayString = "${sendhemosc.disparo-automatico.verificacao-ms:600000}",
            initialDelayString = "${sendhemosc.disparo-automatico.primeira-verificacao-ms:60000}")
    public void verificar() {
        try {
            executarDisparoAutomatico.executarSeDevida(LocalDateTime.now(ConfiguracaoDisparoAutomatico.FUSO));
        } catch (final RuntimeException excecao) {
            // Uma falha nao pode desligar o agendamento: a proxima conferencia tenta de novo.
            log.error("[m=verificar] Falha na rodada automatica de convocacao", excecao);
        }
    }
}
