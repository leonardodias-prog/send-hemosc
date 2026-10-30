package br.univille.sendhemosc.usecase.notificacao;

import br.univille.sendhemosc.domain.dto.EvolucaoNoPeriodo;
import br.univille.sendhemosc.domain.port.outbound.IDoacaoRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.INotificacaoRepositoryPort;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Monta a serie de convocacoes e doacoes do relatorio. Ate 31 dias, um ponto por dia; acima
 * disso, um por semana, para o grafico continuar legivel.
 */
@Service
@RequiredArgsConstructor
public class GerarEvolucaoNoPeriodoUseCase {

    private static final int LIMITE_DIARIO = 31;
    private static final DateTimeFormatter ROTULO = DateTimeFormatter.ofPattern("dd/MM");

    private final INotificacaoRepositoryPort notificacaoRepository;
    private final IDoacaoRepositoryPort doacaoRepository;

    /**
     * Gera a serie.
     *
     * @param inicio data inicial do periodo, inclusive
     * @param fim data final do periodo, inclusive
     * @return a serie, do intervalo mais antigo ao mais recente
     */
    public EvolucaoNoPeriodo execute(final LocalDate inicio, final LocalDate fim) {
        final long dias = ChronoUnit.DAYS.between(inicio, fim) + 1;
        final int passo = dias <= LIMITE_DIARIO ? 1 : 7;
        final int quantidade = (int) Math.ceil(dias / (double) passo);
        final long[] convocacoes = new long[quantidade];
        final long[] doacoes = new long[quantidade];

        notificacaoRepository.listarEnviosNoPeriodo(inicio, fim)
                .forEach(envio -> convocacoes[posicao(inicio, envio.toLocalDate(), passo)]++);
        doacaoRepository.listarDatasNoPeriodo(inicio, fim)
                .forEach(data -> doacoes[posicao(inicio, data, passo)]++);

        final List<EvolucaoNoPeriodo.Ponto> pontos = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            pontos.add(new EvolucaoNoPeriodo.Ponto(inicio.plusDays((long) i * passo).format(ROTULO),
                    convocacoes[i], doacoes[i]));
        }

        return new EvolucaoNoPeriodo(pontos, passo == 1 ? "dia" : "semana");
    }

    private int posicao(final LocalDate inicio, final LocalDate data, final int passo) {
        return (int) (ChronoUnit.DAYS.between(inicio, data) / passo);
    }
}
