package br.univille.sendhemosc.adapter.inbound.scheduler;

import br.univille.sendhemosc.domain.dto.ResultadoConvocacao;
import br.univille.sendhemosc.domain.dto.SituacaoEstoque;
import br.univille.sendhemosc.domain.enums.OrigemNotificacao;
import br.univille.sendhemosc.usecase.estoque.ListarSituacaoEstoqueUseCase;
import br.univille.sendhemosc.usecase.notificacao.ConvocarDoadoresUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Adapter de entrada por agendamento. Varre diariamente a situacao do estoque e dispara a
 * convocacao automatica para os tipos sanguineos em nivel ATENCAO ou CRITICO.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConvocacaoScheduler {

    private final ListarSituacaoEstoqueUseCase listarSituacaoEstoque;
    private final ConvocarDoadoresUseCase convocarDoadores;

    /**
     * Varredura diaria do estoque, executada as 08h00 no horario de Sao Paulo.
     */
    @Scheduled(cron = "${sendhemosc.scheduler.cron-convocacao:0 0 8 * * *}", zone = "America/Sao_Paulo")
    public void convocarTiposEmFalta() {
        final List<SituacaoEstoque> emFalta = listarSituacaoEstoque.execute().stream()
                .filter(situacao -> situacao.nivel().isExigeConvocacao())
                .toList();

        if (emFalta.isEmpty()) {
            log.info("[m=convocarTiposEmFalta] Nenhum tipo sanguineo em nivel baixo, nada a fazer");
            return;
        }

        log.info("[m=convocarTiposEmFalta] {} tipo(s) em nivel baixo, iniciando convocacao automatica", emFalta.size());

        for (final SituacaoEstoque situacao : emFalta) {
            final ResultadoConvocacao resultado = convocarDoadores.execute(
                    situacao.tipoSanguineo(), OrigemNotificacao.AUTOMATICA, false);

            log.info("[m=convocarTiposEmFalta] Tipo {}: {} aptos, {} enviados, {} falhas",
                    resultado.tipoSanguineo().getSigla(), resultado.totalElegiveis(),
                    resultado.totalEnviados(), resultado.totalFalhas());
        }
    }
}
