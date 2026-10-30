package br.univille.sendhemosc.usecase.notificacao;

import br.univille.sendhemosc.domain.dto.PlanoDeRodada;
import br.univille.sendhemosc.domain.dto.ResultadoConvocacao;
import br.univille.sendhemosc.domain.enums.OrigemNotificacao;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import java.time.LocalDate;
import java.util.EnumSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Convocacao geral do painel: chama, de uma vez, os aptos para todo tipo sanguineo em nivel de
 * atencao ou critico.
 *
 * <p>Segue o mesmo plano da rodada automatica: cada pessoa recebe um convite so, pelo tipo mais
 * critico com que e compativel, e os limites de contato valem igual. A diferenca e que nao ha
 * limite por rodada: quem aperta o botao viu o numero na confirmacao. Para convocar um tipo ou
 * pessoas especificas, a tela de doadores tem os filtros.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConvocarTiposEmFaltaUseCase {

    private final ExecutarDisparoAutomaticoUseCase disparoAutomatico;
    private final ConvocarDoadoresUseCase convocarDoadores;
    private final IAuditoriaPort auditoria;

    /**
     * Quem seria convocado agora. Nao envia nada.
     *
     * @param referencia data da convocacao
     * @return o plano, do tipo mais critico ao menos critico
     */
    public PlanoDeRodada planejar(final LocalDate referencia) {
        return disparoAutomatico.planejar(EnumSet.allOf(TipoSanguineo.class), Integer.MAX_VALUE, referencia);
    }

    /**
     * Envia as convocacoes do plano.
     *
     * @param referencia data da convocacao
     * @return totais da convocacao
     */
    public Resultado execute(final LocalDate referencia) {
        final PlanoDeRodada plano = planejar(referencia);
        int enviados = 0;
        int falhas = 0;
        boolean interrompida = false;

        for (final PlanoDeRodada.Item item : plano.itens()) {
            if (item.destinatarios().isEmpty()) {
                continue;
            }

            final ResultadoConvocacao resultado = convocarDoadores.enviar(
                    item.selecao(), item.destinatarios(), OrigemNotificacao.MANUAL);
            enviados += resultado.totalEnviados();
            falhas += resultado.totalFalhas();

            // Falha em sequencia e problema de infraestrutura: o tipo seguinte falharia igual.
            if (resultado.interrompida()) {
                interrompida = true;
                break;
            }
        }

        final Resultado resultado = new Resultado(plano.itens().size(), enviados, falhas, plano.totalRetidos(),
                interrompida);

        if (!plano.itens().isEmpty()) {
            auditoria.registrar("CONVOCACAO_GERAL",
                    "%d tipo(s) em nivel baixo: %d convocacao(oes) enviada(s), %d falha(s), %d retido(s) "
                            .formatted(resultado.tipos(), enviados, falhas, plano.totalRetidos())
                            + "pelo limite de contato" + (interrompida ? ". Interrompida por falhas seguidas." : "."));
        }

        log.info("[m=execute] Convocacao geral: {}", resultado);
        return resultado;
    }

    /**
     * Totais da convocacao geral.
     *
     * @param tipos tipos sanguineos em nivel baixo
     * @param enviados convocacoes enviadas
     * @param falhas envios que falharam
     * @param retidos aptos deixados de fora pelo limite de contato
     * @param interrompida true quando parou por falhas seguidas no envio
     */
    public record Resultado(int tipos, int enviados, int falhas, int retidos, boolean interrompida) {
    }
}
