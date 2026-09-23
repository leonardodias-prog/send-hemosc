package br.univille.sendhemosc.usecase.notificacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.univille.sendhemosc.domain.dto.CandidatoConvocacao;
import br.univille.sendhemosc.domain.dto.ConfiguracaoDisparoAutomatico;
import br.univille.sendhemosc.domain.dto.PlanoDeRodada;
import br.univille.sendhemosc.domain.dto.ResultadoConvocacao;
import br.univille.sendhemosc.domain.dto.SelecaoConvocacao;
import br.univille.sendhemosc.domain.dto.SituacaoEstoque;
import br.univille.sendhemosc.domain.enums.NivelEstoque;
import br.univille.sendhemosc.domain.enums.OrigemNotificacao;
import br.univille.sendhemosc.domain.enums.Sexo;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import br.univille.sendhemosc.domain.port.outbound.IDisparoAutomaticoRepositoryPort;
import br.univille.sendhemosc.usecase.estoque.ListarSituacaoEstoqueUseCase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Rodada automatica de convocacao")
class ExecutarDisparoAutomaticoUseCaseTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 9, 23, 10, 12, 45, 123_456_789);
    private static final LocalDateTime ONTEM = AGORA.minusDays(1).withHour(8);

    private static final SituacaoEstoque O_NEGATIVO =
            new SituacaoEstoque(TipoSanguineo.O_NEGATIVO, 6, 60, 10, NivelEstoque.CRITICO);
    private static final SituacaoEstoque A_NEGATIVO =
            new SituacaoEstoque(TipoSanguineo.A_NEGATIVO, 14, 50, 28, NivelEstoque.CRITICO);
    private static final SituacaoEstoque B_NEGATIVO =
            new SituacaoEstoque(TipoSanguineo.B_NEGATIVO, 15, 30, 50, NivelEstoque.ATENCAO);
    private static final SituacaoEstoque A_POSITIVO =
            new SituacaoEstoque(TipoSanguineo.A_POSITIVO, 90, 120, 75, NivelEstoque.NORMAL);

    @Mock
    private IDisparoAutomaticoRepositoryPort disparoRepository;
    @Mock
    private ListarSituacaoEstoqueUseCase listarSituacaoEstoque;
    @Mock
    private ConvocarDoadoresUseCase convocarDoadores;
    @Mock
    private IAuditoriaPort auditoria;

    private ExecutarDisparoAutomaticoUseCase useCase;

    @BeforeEach
    void preparar() {
        useCase = new ExecutarDisparoAutomaticoUseCase(disparoRepository, listarSituacaoEstoque,
                convocarDoadores, auditoria);
    }

    private ConfiguracaoDisparoAutomatico ligado(final Set<TipoSanguineo> tipos, final int limite) {
        return new ConfiguracaoDisparoAutomatico(true, tipos, limite, 8, 18, 1, ONTEM, null,
                "chefe@example.org", ONTEM);
    }

    private ConfiguracaoDisparoAutomatico todosOsTipos(final int limite) {
        return ligado(EnumSet.allOf(TipoSanguineo.class), limite);
    }

    private static CandidatoConvocacao doador(final long id) {
        return new CandidatoConvocacao(id, "Doador " + id, "d" + id + "@example.org", TipoSanguineo.O_NEGATIVO,
                "tok-" + id, true, Sexo.FEMININO, LocalDate.of(1990, 1, 1), BigDecimal.valueOf(60),
                null, 0, 0, null);
    }

    private static List<CandidatoConvocacao> doadores(final long... ids) {
        return java.util.Arrays.stream(ids).mapToObj(ExecutarDisparoAutomaticoUseCaseTest::doador).toList();
    }

    private void selecaoDe(final SituacaoEstoque situacao, final List<CandidatoConvocacao> elegiveis,
                           final List<CandidatoConvocacao> retidos) {
        when(convocarDoadores.selecionar(eq(situacao), any()))
                .thenReturn(new SelecaoConvocacao(situacao, elegiveis, retidos));
    }

    @Nested
    @DisplayName("planejamento")
    class Planejamento {

        @Test
        @DisplayName("so entram os tipos marcados que estao em nivel baixo, do mais critico ao menos")
        void tiposEscolhidosEmFalta() {
            when(listarSituacaoEstoque.execute()).thenReturn(List.of(O_NEGATIVO, A_NEGATIVO, B_NEGATIVO, A_POSITIVO));
            selecaoDe(O_NEGATIVO, doadores(1), List.of());
            selecaoDe(B_NEGATIVO, doadores(2), List.of());

            // A- esta critico mas nao foi marcado; A+ foi marcado mas esta normal.
            final PlanoDeRodada plano = useCase.planejar(ligado(EnumSet.of(TipoSanguineo.O_NEGATIVO,
                    TipoSanguineo.B_NEGATIVO, TipoSanguineo.A_POSITIVO), 50), AGORA.toLocalDate());

            assertThat(plano.itens())
                    .extracting(item -> item.selecao().situacao().tipoSanguineo())
                    .containsExactly(TipoSanguineo.O_NEGATIVO, TipoSanguineo.B_NEGATIVO);
        }

        @Test
        @DisplayName("o limite da rodada vale para a soma dos tipos, e o mais critico e atendido primeiro")
        void limiteSomaOsTipos() {
            when(listarSituacaoEstoque.execute()).thenReturn(List.of(O_NEGATIVO, B_NEGATIVO));
            selecaoDe(O_NEGATIVO, doadores(1, 2), List.of());
            selecaoDe(B_NEGATIVO, doadores(3, 4, 5), List.of());

            final PlanoDeRodada plano = useCase.planejar(todosOsTipos(3), AGORA.toLocalDate());

            assertThat(plano.itens().get(0).destinatarios()).extracting(CandidatoConvocacao::id).containsExactly(1L, 2L);
            assertThat(plano.itens().get(1).destinatarios()).extracting(CandidatoConvocacao::id).containsExactly(3L);
            assertThat(plano.totalDestinatarios()).isEqualTo(3);
            assertThat(plano.totalAdiados()).as("4 e 5 ficam para a proxima rodada").isEqualTo(2);
        }

        @Test
        @DisplayName("quem e compativel com mais de um tipo em falta recebe um convite so, pelo mais critico")
        void conviteUnico() {
            when(listarSituacaoEstoque.execute()).thenReturn(List.of(O_NEGATIVO, A_NEGATIVO));
            // O doador 7 (O-) doa para O- e para A-: aparece nas duas selecoes.
            selecaoDe(O_NEGATIVO, doadores(7), List.of());
            selecaoDe(A_NEGATIVO, doadores(7, 8), List.of());

            final PlanoDeRodada plano = useCase.planejar(todosOsTipos(50), AGORA.toLocalDate());

            assertThat(plano.itens().get(0).destinatarios()).extracting(CandidatoConvocacao::id).containsExactly(7L);
            assertThat(plano.itens().get(1).destinatarios()).extracting(CandidatoConvocacao::id).containsExactly(8L);
            assertThat(plano.totalDestinatarios()).isEqualTo(2);
        }

        @Test
        @DisplayName("retidos pelo limite de contato sao contados uma vez por pessoa")
        void retidosSemDuplicar() {
            when(listarSituacaoEstoque.execute()).thenReturn(List.of(O_NEGATIVO, A_NEGATIVO));
            selecaoDe(O_NEGATIVO, List.of(), doadores(9));
            selecaoDe(A_NEGATIVO, List.of(), doadores(9));

            assertThat(useCase.planejar(todosOsTipos(50), AGORA.toLocalDate()).totalRetidos()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("execucao")
    class Execucao {

        @Test
        @DisplayName("fora da hora nada acontece: nem assume a rodada, nem consulta o estoque")
        void naoDevida() {
            when(disparoRepository.buscar()).thenReturn(todosOsTipos(50));

            assertThat(useCase.executarSeDevida(AGORA.withHour(22))).isFalse();

            verify(disparoRepository, never()).assumirRodada(any(), any());
            verify(listarSituacaoEstoque, never()).execute();
        }

        @Test
        @DisplayName("outra verificacao assumiu a rodada primeiro: esta nao envia nada")
        void rodadaJaAssumida() {
            when(disparoRepository.buscar()).thenReturn(todosOsTipos(50));
            when(disparoRepository.assumirRodada(ONTEM, AGORA.withNano(0))).thenReturn(false);

            assertThat(useCase.executarSeDevida(AGORA)).isFalse();

            verify(convocarDoadores, never()).enviar(any(), any(), any());
        }

        @Test
        @DisplayName("rodada devida: envia o plano como convocacao automatica e registra quem ligou")
        void executaERegistra() {
            when(disparoRepository.buscar()).thenReturn(todosOsTipos(50));
            when(disparoRepository.assumirRodada(ONTEM, AGORA.withNano(0))).thenReturn(true);
            when(listarSituacaoEstoque.execute()).thenReturn(List.of(O_NEGATIVO));
            selecaoDe(O_NEGATIVO, doadores(1, 2), List.of());
            when(convocarDoadores.enviar(any(), any(), eq(OrigemNotificacao.AUTOMATICA)))
                    .thenReturn(new ResultadoConvocacao(TipoSanguineo.O_NEGATIVO, NivelEstoque.CRITICO,
                            2, 2, 0, 0, false));

            assertThat(useCase.executarSeDevida(AGORA)).isTrue();

            verify(disparoRepository).registrarResumoDaRodada(argThat(resumo -> resumo.contains("2 convocacao")));
            verify(auditoria).registrar(eq("CONVOCACAO_AUTOMATICA"),
                    argThat(detalhe -> detalhe.contains("Configurado por chefe@example.org")));
        }

        @Test
        @DisplayName("falhas seguidas num tipo encerram a rodada: o tipo seguinte falharia igual")
        void interrompidaNaoSegue() {
            when(disparoRepository.buscar()).thenReturn(todosOsTipos(50));
            when(disparoRepository.assumirRodada(any(), any())).thenReturn(true);
            when(listarSituacaoEstoque.execute()).thenReturn(List.of(O_NEGATIVO, B_NEGATIVO));
            selecaoDe(O_NEGATIVO, doadores(1), List.of());
            selecaoDe(B_NEGATIVO, doadores(2), List.of());
            when(convocarDoadores.enviar(any(), any(), any()))
                    .thenReturn(new ResultadoConvocacao(TipoSanguineo.O_NEGATIVO, NivelEstoque.CRITICO,
                            1, 0, 5, 0, true));

            useCase.executarSeDevida(AGORA);

            verify(convocarDoadores, times(1)).enviar(any(), any(), any());
            verify(disparoRepository).registrarResumoDaRodada(argThat(resumo -> resumo.contains("Interrompida")));
        }

        @Test
        @DisplayName("nenhum tipo marcado em nivel baixo: a rodada conta, mas nao gera auditoria")
        void nadaAConvocar() {
            when(disparoRepository.buscar()).thenReturn(todosOsTipos(50));
            when(disparoRepository.assumirRodada(any(), any())).thenReturn(true);
            when(listarSituacaoEstoque.execute()).thenReturn(List.of(A_POSITIVO));

            assertThat(useCase.executarSeDevida(AGORA)).isTrue();

            verify(disparoRepository).registrarResumoDaRodada(argThat(resumo -> resumo.contains("nada a convocar")));
            verify(auditoria, never()).registrar(anyString(), anyString());
        }

        @Test
        @DisplayName("erro inesperado fica registrado no resumo, e nao e engolido")
        void erroInesperado() {
            when(disparoRepository.buscar()).thenReturn(todosOsTipos(50));
            when(disparoRepository.assumirRodada(any(), any())).thenReturn(true);
            when(listarSituacaoEstoque.execute()).thenThrow(new IllegalStateException("banco fora do ar"));

            assertThatThrownBy(() -> useCase.executarSeDevida(AGORA)).isInstanceOf(IllegalStateException.class);

            verify(disparoRepository).registrarResumoDaRodada(argThat(resumo -> resumo.contains("banco fora do ar")));
        }
    }
}
