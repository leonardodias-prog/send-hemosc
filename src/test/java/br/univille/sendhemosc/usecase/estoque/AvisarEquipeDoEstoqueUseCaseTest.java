package br.univille.sendhemosc.usecase.estoque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.univille.sendhemosc.config.SendHemoscProperties;
import br.univille.sendhemosc.domain.dto.ConfiguracaoAvisoEstoque;
import br.univille.sendhemosc.domain.dto.EnvioAvisoEstoque;
import br.univille.sendhemosc.domain.dto.MensagemEmail;
import br.univille.sendhemosc.domain.enums.NivelEstoque;
import br.univille.sendhemosc.domain.enums.TipoAvisoEstoque;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.exception.EstoqueErrorsMessage;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IAvisoEstoqueRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.IEmailPort;
import br.univille.sendhemosc.domain.port.outbound.IEstoqueRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.IEstoqueRepositoryPort.RegistroEstoque;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Cobre as duas regras que o aviso interno nao pode errar: o alerta sai quando o tipo cai de
 * nivel e nao se repete, e o relatorio sai uma vez por dia, a partir do horario.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Aviso interno de estoque")
class AvisarEquipeDoEstoqueUseCaseTest {

    private static final TipoSanguineo TIPO = TipoSanguineo.O_NEGATIVO;
    private static final int MINIMO_CRITICO = 5;
    private static final int MINIMO_BAIXO = 10;
    private static final LocalDateTime SETE_E_DEZ = LocalDateTime.of(2026, 11, 12, 7, 10);

    @Mock
    private IEstoqueRepositoryPort estoqueRepository;
    @Mock
    private IAvisoEstoqueRepositoryPort avisoRepository;
    @Mock
    private IUsuarioRepositoryPort usuarioRepository;
    @Mock
    private ListarSituacaoEstoqueUseCase listarSituacaoEstoque;
    @Mock
    private IEmailPort emailPort;
    @Mock
    private TemplateEngine templateEngine;

    private AvisarEquipeDoEstoqueUseCase useCase;

    @BeforeEach
    void preparar() {
        final var properties = new SendHemoscProperties(null,
                new SendHemoscProperties.Notificacao(30, 3, 180, "teste@example.org", "Teste", "http://localhost:8080"));

        useCase = new AvisarEquipeDoEstoqueUseCase(estoqueRepository, avisoRepository, usuarioRepository,
                listarSituacaoEstoque,
                new ClassificarNivelEstoqueUseCase(), emailPort, templateEngine, properties);

        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>corpo</html>");
        when(listarSituacaoEstoque.execute()).thenReturn(List.of());
        when(usuarioRepository.emailsDosAdministradores())
                .thenReturn(List.of("plantao@example.org", "chefia@example.org"));
        ultimoRelatorio(null);
    }

    private void ultimoRelatorio(final LocalDate dia) {
        when(avisoRepository.buscar()).thenReturn(new ConfiguracaoAvisoEstoque(7, dia, null, null));
    }

    private void estoque(final int bolsas, final NivelEstoque jaAvisado) {
        when(estoqueRepository.buscarPorTipo(TIPO))
                .thenReturn(Optional.of(new RegistroEstoque(TIPO, bolsas, 60, MINIMO_CRITICO, MINIMO_BAIXO)));
        when(estoqueRepository.nivelAvisado(TIPO)).thenReturn(Optional.ofNullable(jaAvisado));
    }

    private List<MensagemEmail> enviadas(final int quantas) {
        final ArgumentCaptor<MensagemEmail> mensagens = ArgumentCaptor.forClass(MensagemEmail.class);
        verify(emailPort, times(quantas)).enviar(mensagens.capture());
        return mensagens.getAllValues();
    }

    @Nested
    @DisplayName("alerta imediato")
    class AlertaImediato {

        @Test
        @DisplayName("tipo que cai para critico avisa todos os administradores, com o tipo e as bolsas no assunto")
        void quedaParaCriticoAvisa() {
            estoque(4, null);

            assertThat(useCase.avaliarAlerta(TIPO)).isTrue();

            assertThat(enviadas(2))
                    .extracting(MensagemEmail::destinatario, MensagemEmail::assunto)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple("plantao@example.org", "Estoque crítico: O- com 4 bolsas"),
                            org.assertj.core.groups.Tuple.tuple("chefia@example.org", "Estoque crítico: O- com 4 bolsas"));
            verify(estoqueRepository).registrarNivelAvisado(TIPO, NivelEstoque.CRITICO);
        }

        @Test
        @DisplayName("tipo que cai para baixo avisa como baixo")
        void quedaParaBaixoAvisa() {
            estoque(8, null);

            assertThat(useCase.avaliarAlerta(TIPO)).isTrue();

            assertThat(enviadas(2).getFirst().assunto()).isEqualTo("Estoque baixo: O- com 8 bolsas");
            verify(estoqueRepository).registrarNivelAvisado(TIPO, NivelEstoque.ATENCAO);
        }

        @Test
        @DisplayName("continuar no mesmo nivel nao repete o alerta")
        void mesmoNivelNaoRepete() {
            estoque(3, NivelEstoque.CRITICO);

            assertThat(useCase.avaliarAlerta(TIPO)).isFalse();

            verify(emailPort, never()).enviar(any());
            verify(estoqueRepository, never()).registrarNivelAvisado(any(), any());
        }

        @Test
        @DisplayName("piorar de baixo para critico avisa de novo")
        void piorarAvisaDeNovo() {
            estoque(4, NivelEstoque.ATENCAO);

            assertThat(useCase.avaliarAlerta(TIPO)).isTrue();

            enviadas(2);
            verify(estoqueRepository).registrarNivelAvisado(TIPO, NivelEstoque.CRITICO);
        }

        @Test
        @DisplayName("voltar ao normal nao manda e-mail, mas rearma o alerta")
        void voltarAoNormalRearma() {
            estoque(15, NivelEstoque.CRITICO);

            assertThat(useCase.avaliarAlerta(TIPO)).isFalse();

            verify(emailPort, never()).enviar(any());
            verify(estoqueRepository).registrarNivelAvisado(TIPO, NivelEstoque.NORMAL);
        }

        @Test
        @DisplayName("melhorar de critico para baixo nao avisa, e uma nova queda a critico volta a avisar")
        void melhorarParaBaixoNaoAvisa() {
            estoque(8, NivelEstoque.CRITICO);

            assertThat(useCase.avaliarAlerta(TIPO)).isFalse();

            verify(emailPort, never()).enviar(any());
            verify(estoqueRepository).registrarNivelAvisado(TIPO, NivelEstoque.ATENCAO);
        }

        @Test
        @DisplayName("falha em um endereco nao impede o envio aos demais, e fica registrada")
        void falhaEmUmNaoCalaOsDemais() {
            estoque(4, null);
            doThrow(new IllegalStateException("caixa cheia")).when(emailPort)
                    .enviar(new MensagemEmail("plantao@example.org", "Estoque crítico: O- com 4 bolsas", "<html>corpo</html>"));

            assertThat(useCase.avaliarAlerta(TIPO)).isTrue();

            enviadas(2);
            final ArgumentCaptor<EnvioAvisoEstoque> registro = ArgumentCaptor.forClass(EnvioAvisoEstoque.class);
            verify(avisoRepository).registrarEnvio(registro.capture());
            assertThat(registro.getValue().tipo()).isEqualTo(TipoAvisoEstoque.ALERTA);
            assertThat(registro.getValue().destinatarios()).isEqualTo(2);
            assertThat(registro.getValue().falhas()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("relatorio diario")
    class RelatorioDiario {

        @Test
        @DisplayName("a partir do horario, sai uma vez com a data no assunto")
        void saiAPartirDoHorario() {
            when(avisoRepository.assumirRelatorioDoDia(SETE_E_DEZ.toLocalDate())).thenReturn(true);

            assertThat(useCase.enviarRelatorioSeDevido(SETE_E_DEZ)).isTrue();

            assertThat(enviadas(2).getFirst().assunto()).isEqualTo("Estoque de sangue – 12/11/2026");
        }

        @Test
        @DisplayName("antes do horario nao sai")
        void antesDoHorarioNaoSai() {
            assertThat(useCase.enviarRelatorioSeDevido(SETE_E_DEZ.withHour(6))).isFalse();

            verify(emailPort, never()).enviar(any());
            verify(avisoRepository, never()).assumirRelatorioDoDia(any());
        }

        @Test
        @DisplayName("o do dia ja saiu: nao sai outro")
        void naoRepeteNoMesmoDia() {
            ultimoRelatorio(SETE_E_DEZ.toLocalDate());

            assertThat(useCase.enviarRelatorioSeDevido(SETE_E_DEZ.withHour(15))).isFalse();

            verify(emailPort, never()).enviar(any());
        }

        @Test
        @DisplayName("servico parado no horario: sai na primeira conferencia depois, ainda no mesmo dia")
        void saiDepoisSeEstavaParado() {
            ultimoRelatorio(SETE_E_DEZ.toLocalDate().minusDays(1));
            when(avisoRepository.assumirRelatorioDoDia(SETE_E_DEZ.toLocalDate())).thenReturn(true);

            assertThat(useCase.enviarRelatorioSeDevido(SETE_E_DEZ.withHour(11))).isTrue();

            enviadas(2);
        }

        @Test
        @DisplayName("sem administrador ativo, nao sai nem gasta o relatorio do dia")
        void semDestinatarioNaoGastaODia() {
            when(usuarioRepository.emailsDosAdministradores()).thenReturn(List.of());

            assertThat(useCase.enviarRelatorioSeDevido(SETE_E_DEZ)).isFalse();

            verify(avisoRepository, never()).assumirRelatorioDoDia(any());
        }
    }

    @Test
    @DisplayName("sem administrador ativo o teste e recusado, em vez de dizer que enviou para ninguem")
    void testeRecusaListaVazia() {
        when(usuarioRepository.emailsDosAdministradores()).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.enviarTeste(SETE_E_DEZ))
                .isInstanceOfSatisfying(NegocioException.class,
                        excecao -> assertThat(excecao.getErro()).isEqualTo(EstoqueErrorsMessage.AVISO_SEM_DESTINATARIOS));
    }
}
