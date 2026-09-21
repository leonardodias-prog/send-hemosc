package br.univille.sendhemosc.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;

@DisplayName("Informacao de configuracao de envio")
class InfoEnvioContributorTest {

    @SuppressWarnings("unchecked")
    private Map<String, Object> detalhes(final String modo, final String destinatarios) {
        final Info.Builder builder = new Info.Builder();
        new InfoEnvioContributor(modo, destinatarios).contribute(builder);

        return (Map<String, Object>) builder.build().getDetails().get("email");
    }

    @Test
    @DisplayName("modo log e reportado como envio desligado")
    void modoLog() {
        final Map<String, Object> email = detalhes("log", "");

        assertThat(email).containsEntry("modo", "log").containsEntry("envioReal", false);
        assertThat(email).containsEntry("redirecionamentoAtivo", false);
    }

    @Test
    @DisplayName("modo smtp e reportado como envio real")
    void modoSmtp() {
        final Map<String, Object> email = detalhes("smtp", "leo@gmail.com");

        assertThat(email).containsEntry("modo", "smtp").containsEntry("envioReal", true);
        assertThat(email).containsEntry("redirecionamentoAtivo", true);
        assertThat(email).containsEntry("totalDestinatariosDeTeste", 1);
    }

    @Test
    @DisplayName("o endereco aparece mascarado, nunca por inteiro")
    void mascaraEndereco() {
        final Map<String, Object> email = detalhes("smtp", "leonardo@gmail.com");

        assertThat(email.get("destinatariosDeTeste").toString())
                .contains("le***@gmail.com")
                .doesNotContain("leonardo@gmail.com");
    }

    @Test
    @DisplayName("conta corretamente varios destinatarios")
    void varios() {
        final Map<String, Object> email = detalhes("smtp", "um@a.com, dois@b.com ,tres@c.com");

        assertThat(email).containsEntry("totalDestinatariosDeTeste", 3);
        assertThat((List<String>) email.get("destinatariosDeTeste")).hasSize(3);
    }

    @Test
    @DisplayName("endereco sem arroba nao vaza nada")
    void enderecoInvalido() {
        final Map<String, Object> email = detalhes("smtp", "semarroba");

        assertThat(email.get("destinatariosDeTeste").toString()).doesNotContain("semarroba");
    }
}
