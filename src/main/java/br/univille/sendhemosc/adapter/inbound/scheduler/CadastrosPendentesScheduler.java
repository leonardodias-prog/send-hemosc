package br.univille.sendhemosc.adapter.inbound.scheduler;

import br.univille.sendhemosc.usecase.usuario.AvisarCadastrosPendentesUseCase;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Adapter de entrada por agendamento: manda o aviso de cadastros pendentes que ficou para depois.
 *
 * <p>O aviso sai no maximo uma vez por periodo. O cadastro que chega dentro do periodo nao e
 * avisado na hora; esta conferencia manda o aviso dele assim que o periodo termina, sem
 * depender de outro cadastro acontecer.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CadastrosPendentesScheduler {

    private final AvisarCadastrosPendentesUseCase avisarCadastrosPendentes;

    @Scheduled(fixedDelayString = "${sendhemosc.seguranca.aprovacao.aviso-verificacao-ms:600000}",
            initialDelayString = "${sendhemosc.seguranca.aprovacao.aviso-primeira-verificacao-ms:60000}")
    public void verificar() {
        try {
            avisarCadastrosPendentes.executarSeDevido(LocalDateTime.now());
        } catch (final RuntimeException excecao) {
            // Uma falha nao pode desligar o agendamento: a proxima conferencia tenta de novo.
            log.error("[m=verificar] Falha no aviso de cadastros pendentes", excecao);
        }
    }
}
