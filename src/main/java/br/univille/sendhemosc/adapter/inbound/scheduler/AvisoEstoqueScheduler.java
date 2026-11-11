package br.univille.sendhemosc.adapter.inbound.scheduler;

import br.univille.sendhemosc.domain.dto.ConfiguracaoDisparoAutomatico;
import br.univille.sendhemosc.usecase.estoque.AvisarEquipeDoEstoqueUseCase;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Adapter de entrada por agendamento: confere de tempos em tempos se o relatorio diario de
 * estoque e devido.
 *
 * <p>Confere em vez de marcar hora fixa pela mesma razao do disparo automatico: o servico hiberna
 * na hospedagem gratuita, e o relatorio precisa sair na primeira oportunidade depois do horario.
 * Se ele sai, a que horas e para quem, quem decide e a configuracao feita na tela.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AvisoEstoqueScheduler {

    private final AvisarEquipeDoEstoqueUseCase avisarEquipe;

    @Scheduled(fixedDelayString = "${sendhemosc.aviso-estoque.verificacao-ms:600000}",
            initialDelayString = "${sendhemosc.aviso-estoque.primeira-verificacao-ms:60000}")
    public void verificar() {
        try {
            avisarEquipe.enviarRelatorioSeDevido(LocalDateTime.now(ConfiguracaoDisparoAutomatico.FUSO));
        } catch (final RuntimeException excecao) {
            // Uma falha nao pode desligar o agendamento: a proxima conferencia tenta de novo.
            log.error("[m=verificar] Falha no relatorio diario de estoque", excecao);
        }
    }
}
