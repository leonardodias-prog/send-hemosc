package br.univille.sendhemosc.usecase.notificacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.univille.sendhemosc.config.SendHemoscProperties;
import br.univille.sendhemosc.domain.dto.CandidatoConvocacao;
import br.univille.sendhemosc.domain.dto.MensagemEmail;
import br.univille.sendhemosc.domain.dto.ResultadoConvocacao;
import br.univille.sendhemosc.domain.enums.NivelEstoque;
import br.univille.sendhemosc.domain.enums.OrigemNotificacao;
import br.univille.sendhemosc.domain.enums.Sexo;
import br.univille.sendhemosc.domain.enums.StatusNotificacao;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.IEmailPort;
import br.univille.sendhemosc.domain.port.outbound.IEstoqueRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.INotificacaoRepositoryPort;
import br.univille.sendhemosc.usecase.doador.CalcularAptidaoUseCase;
import br.univille.sendhemosc.usecase.estoque.ClassificarNivelEstoqueUseCase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Convocacao segmentada de doadores")
class ConvocarDoadoresUseCaseTest {

    @Mock
    private IEstoqueRepositoryPort estoqueRepository;
    @Mock
    private IDoadorRepositoryPort doadorRepository;
    @Mock
    private INotificacaoRepositoryPort notificacaoRepository;
    @Mock
    private IEmailPort emailPort;
    @Mock
    private RenderizarConvocacaoUseCase renderizarConvocacao;

    private ConvocarDoadoresUseCase useCase;

    @BeforeEach
    void preparar() {
        final var properties = new SendHemoscProperties(
                new SendHemoscProperties.Aptidao(60, 90, 4, 3, 16, 69, BigDecimal.valueOf(50)),
                new SendHemoscProperties.Estoque(30, 60),
                new SendHemoscProperties.Notificacao(30, "teste@example.org", "Teste", "http://localhost:8080"));

        useCase = new ConvocarDoadoresUseCase(
                estoqueRepository, doadorRepository, notificacaoRepository, emailPort,
                new ClassificarNivelEstoqueUseCase(properties), new CalcularAptidaoUseCase(properties),
                renderizarConvocacao);
    }

    private CandidatoConvocacao candidato(final long id, final LocalDate ultimaDoacao) {
        return new CandidatoConvocacao(id, "Doador " + id, "doador" + id + "@example.org",
                TipoSanguineo.O_NEGATIVO, "token-" + id, Sexo.MASCULINO,
                LocalDate.now().minusYears(30), BigDecimal.valueOf(75), ultimaDoacao, 0);
    }

    private void estoqueEm(final int quantidade, final int capacidade) {
        when(estoqueRepository.buscarPorTipo(TipoSanguineo.O_NEGATIVO))
                .thenReturn(Optional.of(new IEstoqueRepositoryPort.RegistroEstoque(
                        TipoSanguineo.O_NEGATIVO, quantidade, capacidade)));
    }

    @Test
    @DisplayName("estoque em nivel NORMAL nao dispara convocacao")
    void naoConvocaComEstoqueNormal() {
        estoqueEm(80, 100);

        final ResultadoConvocacao resultado = useCase.execute(
                TipoSanguineo.O_NEGATIVO, OrigemNotificacao.AUTOMATICA, false);

        assertThat(resultado.nivel()).isEqualTo(NivelEstoque.NORMAL);
        assertThat(resultado.totalEnviados()).isZero();
        verify(emailPort, never()).enviar(any());
        verify(doadorRepository, never()).buscarCandidatos(anySet(), any());
    }

    @Test
    @DisplayName("com ignorarNivel a convocacao acontece mesmo em nivel NORMAL")
    void forcaConvocacaoManual() {
        estoqueEm(80, 100);
        when(doadorRepository.buscarCandidatos(anySet(), any())).thenReturn(List.of(candidato(1L, null)));
        when(renderizarConvocacao.execute(any(), any()))
                .thenReturn(new MensagemEmail("doador1@example.org", "assunto", "<p>corpo</p>"));

        final ResultadoConvocacao resultado = useCase.execute(
                TipoSanguineo.O_NEGATIVO, OrigemNotificacao.MANUAL, true);

        assertThat(resultado.totalEnviados()).isEqualTo(1);
        verify(emailPort).enviar(any());
    }

    @Test
    @DisplayName("busca doadores pelos tipos compativeis, nao apenas pelo tipo exato")
    void buscaPorCompatibilidade() {
        when(estoqueRepository.buscarPorTipo(TipoSanguineo.A_POSITIVO))
                .thenReturn(Optional.of(new IEstoqueRepositoryPort.RegistroEstoque(
                        TipoSanguineo.A_POSITIVO, 10, 100)));
        when(doadorRepository.buscarCandidatos(anySet(), any())).thenReturn(List.of());

        useCase.execute(TipoSanguineo.A_POSITIVO, OrigemNotificacao.AUTOMATICA, false);

        final ArgumentCaptor<Set<String>> siglas = ArgumentCaptor.forClass(Set.class);
        verify(doadorRepository).buscarCandidatos(siglas.capture(), any());

        assertThat(siglas.getValue()).containsExactlyInAnyOrder("A+", "A-", "O+", "O-");
    }

    @Test
    @DisplayName("candidato que nao cumpriu o intervalo e descartado antes do envio")
    void filtraInaptos() {
        estoqueEm(10, 100);
        when(doadorRepository.buscarCandidatos(anySet(), any())).thenReturn(List.of(
                candidato(1L, LocalDate.now().minusDays(10)),
                candidato(2L, LocalDate.now().minusDays(120))));
        when(renderizarConvocacao.execute(any(), any()))
                .thenReturn(new MensagemEmail("doador2@example.org", "assunto", "<p>corpo</p>"));

        final ResultadoConvocacao resultado = useCase.execute(
                TipoSanguineo.O_NEGATIVO, OrigemNotificacao.AUTOMATICA, false);

        assertThat(resultado.totalElegiveis()).isEqualTo(1);
        assertThat(resultado.totalEnviados()).isEqualTo(1);
        verify(emailPort, times(1)).enviar(any());
    }

    @Test
    @DisplayName("falha no envio de um doador nao interrompe os demais e fica registrada")
    void falhaIsoladaNaoDerrubaALista() {
        estoqueEm(10, 100);
        when(doadorRepository.buscarCandidatos(anySet(), any())).thenReturn(List.of(
                candidato(1L, null), candidato(2L, null)));
        when(renderizarConvocacao.execute(any(), any()))
                .thenReturn(new MensagemEmail("destino@example.org", "assunto", "<p>corpo</p>"));
        doThrow(new IllegalStateException("smtp indisponivel"))
                .doNothing()
                .when(emailPort).enviar(any());

        final ResultadoConvocacao resultado = useCase.execute(
                TipoSanguineo.O_NEGATIVO, OrigemNotificacao.AUTOMATICA, false);

        assertThat(resultado.totalElegiveis()).isEqualTo(2);
        assertThat(resultado.totalEnviados()).isEqualTo(1);
        assertThat(resultado.totalFalhas()).isEqualTo(1);

        verify(notificacaoRepository).registrar(anyLong(), any(), any(), any(),
                eq(StatusNotificacao.FALHA), anyInt(), eq("smtp indisponivel"));
    }
}
