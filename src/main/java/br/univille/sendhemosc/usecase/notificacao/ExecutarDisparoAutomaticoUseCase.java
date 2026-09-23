package br.univille.sendhemosc.usecase.notificacao;

import br.univille.sendhemosc.domain.dto.CandidatoConvocacao;
import br.univille.sendhemosc.domain.dto.ConfiguracaoDisparoAutomatico;
import br.univille.sendhemosc.domain.dto.PlanoDeRodada;
import br.univille.sendhemosc.domain.dto.ResultadoConvocacao;
import br.univille.sendhemosc.domain.dto.SelecaoConvocacao;
import br.univille.sendhemosc.domain.dto.SituacaoEstoque;
import br.univille.sendhemosc.domain.enums.OrigemNotificacao;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import br.univille.sendhemosc.domain.port.outbound.IDisparoAutomaticoRepositoryPort;
import br.univille.sendhemosc.usecase.estoque.ListarSituacaoEstoqueUseCase;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Rodada automatica de convocacao: decide se ela e devida, monta o plano e envia.
 *
 * <p>O plano sai de uma vez, para todos os tipos escolhidos que estao em falta, antes de qualquer
 * envio. E o que garante tres coisas: o tipo mais critico e atendido primeiro quando o limite da
 * rodada nao da para todos; quem e compativel com mais de um tipo em falta recebe um convite so;
 * e a tela pode mostrar exatamente quem seria convocado, porque usa o mesmo plano.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutarDisparoAutomaticoUseCase {

    /** Tamanho da coluna ultima_rodada_resumo. */
    private static final int TAMANHO_DO_RESUMO = 300;

    private final IDisparoAutomaticoRepositoryPort disparoRepository;
    private final ListarSituacaoEstoqueUseCase listarSituacaoEstoque;
    private final ConvocarDoadoresUseCase convocarDoadores;
    private final IAuditoriaPort auditoria;

    /**
     * Executa a rodada se ela for devida agora. Chamado pelo agendador a cada verificacao.
     *
     * @param agora momento da verificacao, no fuso de Sao Paulo
     * @return true se uma rodada aconteceu
     */
    public boolean executarSeDevida(final LocalDateTime agora) {
        final ConfiguracaoDisparoAutomatico configuracao = disparoRepository.buscar();

        if (!configuracao.rodadaDevida(agora)) {
            return false;
        }

        // Truncar evita comparar instantes com precisoes diferentes na troca condicional seguinte.
        final LocalDateTime inicio = agora.truncatedTo(ChronoUnit.SECONDS);
        if (!disparoRepository.assumirRodada(configuracao.ultimaRodadaEm(), inicio)) {
            log.info("[m=executarSeDevida] Rodada ja assumida por outra verificacao, nada a fazer");
            return false;
        }

        try {
            final String resumo = executar(configuracao, inicio.toLocalDate());
            disparoRepository.registrarResumoDaRodada(resumo);
            log.info("[m=executarSeDevida] {}", resumo);
        } catch (final RuntimeException excecao) {
            disparoRepository.registrarResumoDaRodada(caberNoResumo(
                    "Rodada interrompida por erro inesperado: " + excecao.getMessage()));
            throw excecao;
        }

        return true;
    }

    /**
     * Mensagem de excecao nao tem tamanho previsivel. Sem cortar, gravar o resumo do erro falharia
     * pelo tamanho da coluna, e essa segunda falha esconderia a primeira.
     */
    private String caberNoResumo(final String texto) {
        return texto.length() <= TAMANHO_DO_RESUMO ? texto : texto.substring(0, TAMANHO_DO_RESUMO);
    }

    /**
     * Quem seria convocado se a rodada acontecesse agora. Nao envia nada.
     *
     * @param configuracao configuracao a considerar
     * @param referencia data da rodada
     * @return o plano, do tipo mais critico ao menos critico
     */
    public PlanoDeRodada planejar(final ConfiguracaoDisparoAutomatico configuracao, final LocalDate referencia) {
        // A listagem ja vem ordenada da menor ocupacao para a maior.
        final List<SituacaoEstoque> emFalta = listarSituacaoEstoque.execute().stream()
                .filter(situacao -> situacao.nivel().isExigeConvocacao())
                .filter(situacao -> configuracao.tiposSanguineos().contains(situacao.tipoSanguineo()))
                .toList();

        final List<PlanoDeRodada.Item> itens = new ArrayList<>();
        final Set<Long> destinatarios = new HashSet<>();
        final Set<Long> retidos = new HashSet<>();
        final Set<Long> adiados = new HashSet<>();
        int saldo = configuracao.limitePorRodada();

        for (final SituacaoEstoque situacao : emFalta) {
            final SelecaoConvocacao selecao = convocarDoadores.selecionar(situacao, referencia);
            selecao.retidos().forEach(candidato -> retidos.add(candidato.id()));

            // Quem ja entrou por um tipo mais critico nao recebe um segundo convite na mesma rodada.
            final List<CandidatoConvocacao> novos = selecao.elegiveis().stream()
                    .filter(candidato -> !destinatarios.contains(candidato.id()))
                    .toList();
            final List<CandidatoConvocacao> desteTipo = novos.stream().limit(saldo).toList();

            novos.stream().skip(desteTipo.size()).forEach(candidato -> adiados.add(candidato.id()));
            desteTipo.forEach(candidato -> destinatarios.add(candidato.id()));
            saldo -= desteTipo.size();

            itens.add(new PlanoDeRodada.Item(selecao, desteTipo));
        }

        adiados.removeAll(destinatarios);
        return new PlanoDeRodada(itens, destinatarios.size(), retidos.size(), adiados.size());
    }

    private String executar(final ConfiguracaoDisparoAutomatico configuracao, final LocalDate referencia) {
        final PlanoDeRodada plano = planejar(configuracao, referencia);

        if (plano.itens().isEmpty()) {
            return "Nenhum dos tipos escolhidos estava em nivel baixo: nada a convocar.";
        }

        int enviados = 0;
        int falhas = 0;
        boolean interrompida = false;

        for (final PlanoDeRodada.Item item : plano.itens()) {
            if (item.destinatarios().isEmpty()) {
                continue;
            }

            final ResultadoConvocacao resultado = convocarDoadores.enviar(
                    item.selecao(), item.destinatarios(), OrigemNotificacao.AUTOMATICA);
            enviados += resultado.totalEnviados();
            falhas += resultado.totalFalhas();

            // Falha em sequencia e problema de infraestrutura: o tipo seguinte falharia igual.
            if (resultado.interrompida()) {
                interrompida = true;
                break;
            }
        }

        final String resumo = "%d tipo(s) em nivel baixo: %d convocacao(oes) enviada(s), %d falha(s), "
                .formatted(plano.itens().size(), enviados, falhas)
                + "%d retido(s) pelo limite de contato, %d adiado(s) pelo limite da rodada."
                        .formatted(plano.totalRetidos(), plano.totalAdiados())
                + (interrompida ? " Interrompida por falhas seguidas no envio." : "");

        // O disparo e do sistema, mas a decisao de liga-lo foi de alguem: o rastro fica com o nome.
        auditoria.registrar("CONVOCACAO_AUTOMATICA",
                resumo + " Configurado por " + configuracao.atualizadoPor() + ".");

        return resumo;
    }
}
