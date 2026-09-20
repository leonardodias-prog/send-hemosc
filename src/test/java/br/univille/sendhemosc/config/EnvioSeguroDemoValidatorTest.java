package br.univille.sendhemosc.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Trava de envio da demonstracao publica")
class EnvioSeguroDemoValidatorTest {

    @Test
    @DisplayName("recusa subir com smtp sem destinatario unico")
    void recusaSmtpSemDestinatario() {
        assertThatThrownBy(() -> new EnvioSeguroDemoValidator("smtp", "").validar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EMAIL_DESTINATARIO_TESTE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t"})
    @DisplayName("destinatario em branco nao vale como configurado")
    void brancoNaoConta(final String destinatario) {
        assertThatThrownBy(() -> new EnvioSeguroDemoValidator("smtp", destinatario).validar())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("aceita smtp quando ha destinatario unico")
    void aceitaSmtpComDestinatario() {
        assertThatCode(() -> new EnvioSeguroDemoValidator("smtp", "eu@example.org").validar())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("modo log sobe normalmente, com ou sem destinatario")
    void modoLogSempreSobe() {
        assertThatCode(() -> new EnvioSeguroDemoValidator("log", "").validar())
                .doesNotThrowAnyException();
        assertThatCode(() -> new EnvioSeguroDemoValidator("log", "eu@example.org").validar())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a verificacao do modo ignora diferenca de caixa")
    void modoInsensivelACaixa() {
        assertThatThrownBy(() -> new EnvioSeguroDemoValidator("SMTP", "").validar())
                .isInstanceOf(IllegalStateException.class);
    }
}
