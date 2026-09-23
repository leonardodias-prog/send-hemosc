package br.univille.sendhemosc.usecase.notificacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.univille.sendhemosc.domain.dto.AjusteDisparoAutomatico;
import br.univille.sendhemosc.domain.dto.ConfiguracaoDisparoAutomatico;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import br.univille.sendhemosc.domain.port.outbound.IDisparoAutomaticoRepositoryPort;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Configuracao do disparo automatico")
class ConfigurarDisparoAutomaticoUseCaseTest {

    private static final Set<TipoSanguineo> TODOS = EnumSet.allOf(TipoSanguineo.class);
    private static final String AUTOR = "chefe@example.org";

    @Mock
    private IDisparoAutomaticoRepositoryPort disparoRepository;
    @Mock
    private IAuditoriaPort auditoria;

    private ConfigurarDisparoAutomaticoUseCase useCase;

    @BeforeEach
    void preparar() {
        useCase = new ConfigurarDisparoAutomaticoUseCase(disparoRepository, auditoria);
    }

    private void vigente(final boolean ativo, final int limite) {
        when(disparoRepository.buscar()).thenReturn(new ConfiguracaoDisparoAutomatico(
                ativo, TODOS, limite, 8, 18, 1, null, null, null, null));
    }

    private AjusteDisparoAutomatico ajuste(final boolean ativo, final Set<TipoSanguineo> tipos, final int limite,
                                           final int inicio, final int fim, final int intervalo) {
        return new AjusteDisparoAutomatico(ativo, tipos, limite, inicio, fim, intervalo);
    }

    @Nested
    @DisplayName("validacao")
    class Validacao {

        @Test
        @DisplayName("limite fora de 1 a 1000 e recusado")
        void limite() {
            assertThatThrownBy(() -> useCase.salvar(ajuste(true, TODOS, 0, 8, 18, 1), AUTOR))
                    .isInstanceOf(NegocioException.class).hasMessage("disparo.limite.invalido");
            assertThatThrownBy(() -> useCase.salvar(ajuste(true, TODOS, 1001, 8, 18, 1), AUTOR))
                    .isInstanceOf(NegocioException.class).hasMessage("disparo.limite.invalido");
        }

        @Test
        @DisplayName("janela que termina antes de comecar e recusada")
        void janela() {
            assertThatThrownBy(() -> useCase.salvar(ajuste(true, TODOS, 50, 18, 8, 1), AUTOR))
                    .isInstanceOf(NegocioException.class).hasMessage("disparo.janela.invalida");
            assertThatThrownBy(() -> useCase.salvar(ajuste(true, TODOS, 50, 10, 10, 1), AUTOR))
                    .isInstanceOf(NegocioException.class).hasMessage("disparo.janela.invalida");
        }

        @Test
        @DisplayName("frequencia fora de 1 a 30 dias e recusada")
        void intervalo() {
            assertThatThrownBy(() -> useCase.salvar(ajuste(true, TODOS, 50, 8, 18, 0), AUTOR))
                    .isInstanceOf(NegocioException.class).hasMessage("disparo.intervalo.invalido");
        }

        @Test
        @DisplayName("ligar sem nenhum tipo e recusado: seria uma rodada que nunca convoca")
        void ligarSemTipo() {
            assertThatThrownBy(() -> useCase.salvar(ajuste(true, Set.of(), 50, 8, 18, 1), AUTOR))
                    .isInstanceOf(NegocioException.class).hasMessage("disparo.tipos.vazio");

            verify(disparoRepository, never()).salvar(any(), any(), any());
        }

        @Test
        @DisplayName("desligar sem nenhum tipo marcado e permitido")
        void desligarSemTipo() {
            vigente(true, 50);

            assertThat(useCase.salvar(ajuste(false, Set.of(), 50, 8, 18, 1), AUTOR))
                    .isEqualTo(ConfigurarDisparoAutomaticoUseCase.Alteracao.DESLIGADO);
        }
    }

    @Nested
    @DisplayName("auditoria")
    class Auditoria {

        @Test
        @DisplayName("ligar grava em nome de quem ligou e audita com acao propria")
        void ligar() {
            vigente(false, 50);

            final var alteracao = useCase.salvar(ajuste(true, TODOS, 50, 8, 18, 1), AUTOR);

            assertThat(alteracao).isEqualTo(ConfigurarDisparoAutomaticoUseCase.Alteracao.LIGADO);
            verify(disparoRepository).salvar(any(), eq(AUTOR), any());
            verify(auditoria).registrar(eq("DISPARO_AUTOMATICO_LIGADO"), argThat(detalhe -> detalhe.startsWith("ligado")));
        }

        @Test
        @DisplayName("mudar so o limite e alteracao, e nao liga nem desliga")
        void alterar() {
            vigente(true, 50);

            assertThat(useCase.salvar(ajuste(true, TODOS, 20, 8, 18, 1), AUTOR))
                    .isEqualTo(ConfigurarDisparoAutomaticoUseCase.Alteracao.ALTERADO);
            verify(auditoria).registrar(eq("DISPARO_AUTOMATICO_ALTERADO"), argThat(detalhe -> detalhe.contains("ate 20")));
        }

        @Test
        @DisplayName("salvar sem mudar nada nao grava nem polui a auditoria")
        void semMudanca() {
            vigente(true, 50);

            assertThat(useCase.salvar(ajuste(true, EnumSet.allOf(TipoSanguineo.class), 50, 8, 18, 1), AUTOR))
                    .isEqualTo(ConfigurarDisparoAutomaticoUseCase.Alteracao.NENHUMA);
            verify(disparoRepository, never()).salvar(any(), any(), any());
            verify(auditoria, never()).registrar(anyString(), anyString());
        }
    }
}
