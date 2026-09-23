package br.univille.sendhemosc.usecase.doador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.univille.sendhemosc.config.SendHemoscProperties;
import br.univille.sendhemosc.domain.dto.CandidatoConvocacao;
import br.univille.sendhemosc.domain.dto.NovaDoacao;
import br.univille.sendhemosc.domain.dto.ResultadoRegistroDoacao;
import br.univille.sendhemosc.domain.enums.Sexo;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import br.univille.sendhemosc.domain.port.outbound.IDoacaoRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.INotificacaoRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Cobre o registro da doacao e o fechamento das convocacoes em aberto: o que faltava para o
 * sistema saber quem atendeu ao chamado.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Registro de doacao")
class RegistrarDoacaoUseCaseTest {

    private static final LocalDate HOJE = LocalDate.now();

    @Mock
    private IDoacaoRepositoryPort doacaoRepository;
    @Mock
    private IDoadorRepositoryPort doadorRepository;
    @Mock
    private INotificacaoRepositoryPort notificacaoRepository;
    @Mock
    private IAuditoriaPort auditoria;

    private RegistrarDoacaoUseCase useCase;

    @BeforeEach
    void preparar() {
        final var properties = new SendHemoscProperties(
                new SendHemoscProperties.Aptidao(60, 90, 4, 3, 16, 69, BigDecimal.valueOf(50)),
                new SendHemoscProperties.Estoque(30, 60),
                new SendHemoscProperties.Notificacao(30, 3, "teste@example.org", "Teste", "http://localhost:8080"));

        useCase = new RegistrarDoacaoUseCase(doacaoRepository, doadorRepository,
                notificacaoRepository, auditoria, new CalcularAptidaoUseCase(properties));
    }

    private void doadorExistente(final LocalDate ultimaDoacao, final long doacoesNaJanela) {
        when(doadorRepository.buscarPorIdentificadores(anySet(), any())).thenReturn(List.of(
                new CandidatoConvocacao(1L, "Beatriz Souza", "beatriz@example.org",
                        TipoSanguineo.O_NEGATIVO, "tok", true, Sexo.FEMININO,
                        HOJE.minusYears(30), BigDecimal.valueOf(65), ultimaDoacao, doacoesNaJanela, 0, null)));
    }

    @Nested
    @DisplayName("registro bem sucedido")
    class Sucesso {

        @Test
        @DisplayName("grava a doacao e fecha as convocacoes que estavam em aberto")
        void gravaEFecha() {
            doadorExistente(null, 0);
            when(doacaoRepository.existeNoDia(anyLong(), any())).thenReturn(false);
            when(doacaoRepository.registrar(any())).thenReturn(10L);
            when(notificacaoRepository.marcarComparecimento(1L, HOJE)).thenReturn(3);

            final ResultadoRegistroDoacao resultado =
                    useCase.execute(new NovaDoacao(1L, HOJE, "Agencia Transfusional", null));

            assertThat(resultado.doacaoId()).isEqualTo(10L);
            assertThat(resultado.convocacoesFechadas()).isEqualTo(3);
            assertThat(resultado.nomeDoador()).isEqualTo("Beatriz Souza");
            verify(notificacaoRepository).marcarComparecimento(1L, HOJE);
        }

        @Test
        @DisplayName("calcula a proxima data apta contando a doacao recem registrada")
        void proximaDataConsideraADoacaoNova() {
            doadorExistente(null, 0);
            when(doacaoRepository.existeNoDia(anyLong(), any())).thenReturn(false);
            when(doacaoRepository.registrar(any())).thenReturn(10L);

            final ResultadoRegistroDoacao resultado = useCase.execute(new NovaDoacao(1L, HOJE, null, null));

            // Mulher: intervalo de 90 dias a partir da doacao registrada.
            assertThat(resultado.proximaDataApta()).isEqualTo(HOJE.plusDays(90));
        }

        @Test
        @DisplayName("registra em auditoria quem doou e quantas convocacoes foram fechadas")
        void registraEmAuditoria() {
            doadorExistente(null, 0);
            when(doacaoRepository.existeNoDia(anyLong(), any())).thenReturn(false);
            when(doacaoRepository.registrar(any())).thenReturn(10L);
            when(notificacaoRepository.marcarComparecimento(anyLong(), any())).thenReturn(2);

            useCase.execute(new NovaDoacao(1L, HOJE, null, null));

            verify(auditoria).registrar(anyString(),
                    org.mockito.ArgumentMatchers.argThat(detalhe ->
                            detalhe.contains("Beatriz Souza") && detalhe.contains("2 convocacao")));
        }
    }

    @Nested
    @DisplayName("recusas")
    class Recusas {

        @Test
        @DisplayName("data futura e recusada: doacao nao acontece amanha")
        void dataFutura() {
            assertThatThrownBy(() -> useCase.execute(new NovaDoacao(1L, HOJE.plusDays(1), null, null)))
                    .isInstanceOf(NegocioException.class)
                    .hasMessage("doacao.data.futura");

            verify(doacaoRepository, never()).registrar(any());
        }

        @Test
        @DisplayName("doador inexistente e recusado")
        void doadorInexistente() {
            when(doadorRepository.buscarPorIdentificadores(anySet(), any())).thenReturn(List.of());

            assertThatThrownBy(() -> useCase.execute(new NovaDoacao(99L, HOJE, null, null)))
                    .isInstanceOf(NegocioException.class)
                    .hasMessage("doacao.doador.nao-encontrado");
        }

        @Test
        @DisplayName("segunda doacao no mesmo dia e recusada, para nao distorcer o intervalo")
        void duplicataNoMesmoDia() {
            doadorExistente(null, 0);
            when(doacaoRepository.existeNoDia(1L, HOJE)).thenReturn(true);

            assertThatThrownBy(() -> useCase.execute(new NovaDoacao(1L, HOJE, null, null)))
                    .isInstanceOf(NegocioException.class)
                    .hasMessage("doacao.ja-registrada-no-dia");

            verify(doacaoRepository, never()).registrar(any());
            verify(notificacaoRepository, never()).marcarComparecimento(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("divergencia com a regra de aptidao")
    class Divergencia {

        @Test
        @DisplayName("doacao de quem constava inapto e REGISTRADA, e sinalizada")
        void registraMesmoInapto() {
            // Doou ha 10 dias: pela regra ainda nao poderia doar.
            doadorExistente(HOJE.minusDays(10), 1);
            when(doacaoRepository.existeNoDia(anyLong(), any())).thenReturn(false);
            when(doacaoRepository.registrar(any())).thenReturn(10L);

            final ResultadoRegistroDoacao resultado = useCase.execute(new NovaDoacao(1L, HOJE, null, null));

            assertThat(resultado.estavaApto())
                    .as("o retorno precisa sinalizar a divergencia")
                    .isFalse();
            assertThat(resultado.doacaoId())
                    .as("quem avalia aptidao clinica e a triagem; o sistema registra o que aconteceu")
                    .isEqualTo(10L);
            verify(doacaoRepository).registrar(any());
        }

        @Test
        @DisplayName("doacao de quem estava apto nao levanta sinalizacao")
        void semDivergenciaQuandoApto() {
            doadorExistente(HOJE.minusDays(200), 1);
            when(doacaoRepository.existeNoDia(anyLong(), any())).thenReturn(false);
            when(doacaoRepository.registrar(any())).thenReturn(10L);

            assertThat(useCase.execute(new NovaDoacao(1L, HOJE, null, null)).estavaApto()).isTrue();
        }
    }
}
