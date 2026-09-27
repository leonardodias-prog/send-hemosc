package br.univille.sendhemosc.usecase.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import br.univille.sendhemosc.config.SegurancaProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Limite de tentativas de entrada")
class LimiteDeTentativasDeLoginTest {

    private static final String EMAIL = "pessoa@example.org";

    private RelogioAjustavel relogio;
    private LimiteDeTentativasDeLogin limite;

    @BeforeEach
    void preparar() {
        relogio = new RelogioAjustavel(Instant.parse("2026-09-27T12:00:00Z"));
        limite = new LimiteDeTentativasDeLogin(new SegurancaProperties(
                new SegurancaProperties.Login(5, 15),
                new SegurancaProperties.RecuperacaoSenha(60, 15, 3, 30),
                new SegurancaProperties.Aprovacao(7, 60, 10)), relogio);
    }

    private void falhar(final int vezes) {
        for (int i = 0; i < vezes; i++) {
            limite.registrarFalha(EMAIL);
        }
    }

    @Test
    @DisplayName("quatro falhas seguidas ainda nao bloqueiam")
    void quatroFalhasNaoBloqueiam() {
        falhar(4);

        assertThat(limite.bloqueado(EMAIL)).isFalse();
    }

    @Test
    @DisplayName("a quinta falha seguida bloqueia, e avisa que foi ela que bloqueou")
    void quintaFalhaBloqueia() {
        falhar(4);

        assertThat(limite.registrarFalha(EMAIL)).isTrue();
        assertThat(limite.bloqueado(EMAIL)).isTrue();
    }

    @Test
    @DisplayName("o bloqueio vale para o mesmo e-mail escrito com maiusculas e espacos")
    void mesmoEmailComOutraGrafia() {
        falhar(5);

        assertThat(limite.bloqueado("  PESSOA@Example.ORG ")).isTrue();
    }

    @Test
    @DisplayName("o bloqueio dura quinze minutos; depois, a contagem recomeca do zero")
    void bloqueioAcaba() {
        falhar(5);

        relogio.avancar(Duration.ofMinutes(14));
        assertThat(limite.bloqueado(EMAIL)).isTrue();

        relogio.avancar(Duration.ofMinutes(2));
        assertThat(limite.bloqueado(EMAIL)).isFalse();

        assertThat(limite.registrarFalha(EMAIL)).isFalse();
        assertThat(limite.bloqueado(EMAIL)).isFalse();
    }

    @Test
    @DisplayName("falhas espacadas alem da janela nao se somam")
    void falhasEspacadasNaoSomam() {
        for (int i = 0; i < 6; i++) {
            limite.registrarFalha(EMAIL);
            relogio.avancar(Duration.ofMinutes(16));
        }

        assertThat(limite.bloqueado(EMAIL)).isFalse();
    }

    @Test
    @DisplayName("liberar esquece as falhas: entrada certa ou senha redefinida pelo link")
    void liberarEsquece() {
        falhar(5);

        limite.liberar(EMAIL);

        assertThat(limite.bloqueado(EMAIL)).isFalse();
        falhar(4);
        assertThat(limite.bloqueado(EMAIL)).isFalse();
    }

    @Test
    @DisplayName("um e-mail bloqueado nao bloqueia os outros")
    void bloqueioPorEmail() {
        falhar(5);

        assertThat(limite.bloqueado("outra.pessoa@example.org")).isFalse();
    }

    /** Relogio que o teste adianta a vontade. */
    private static final class RelogioAjustavel extends Clock {

        private Instant agora;

        RelogioAjustavel(final Instant inicio) {
            this.agora = inicio;
        }

        void avancar(final Duration quanto) {
            agora = agora.plus(quanto);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }
}
