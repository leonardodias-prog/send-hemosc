package br.univille.sendhemosc.adapter.outbound.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.univille.sendhemosc.config.SendHemoscProperties;
import br.univille.sendhemosc.domain.dto.MensagemEmail;
import br.univille.sendhemosc.domain.exception.NegocioException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Exercita o adapter contra um servidor HTTP local, verificando o formato exigido pela API do
 * Brevo: remetente em objeto proprio, destinatarios como lista de objetos e o corpo em
 * htmlContent. Formato errado so apareceria em producao, com a mensagem recusada.
 */
@DisplayName("Envio pela API do Brevo")
class BrevoEmailAdapterTest {

    private static final MensagemEmail MENSAGEM =
            new MensagemEmail("doador42@example.org", "Estoque O- critico", "<p>corpo</p>");

    private static final ObjectMapper JSON = new ObjectMapper();

    private HttpServer servidor;
    private String endpoint;
    private final List<JsonNode> corpos = new ArrayList<>();
    private final List<String> chaves = new ArrayList<>();
    private final AtomicInteger statusResposta = new AtomicInteger(201);

    @BeforeEach
    void subirServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/v3/smtp/email", troca -> {
            chaves.add(troca.getRequestHeaders().getFirst("api-key"));
            corpos.add(JSON.readTree(troca.getRequestBody().readAllBytes()));

            final byte[] resposta = "{\"messageId\":\"<1@brevo>\"}".getBytes(StandardCharsets.UTF_8);
            troca.sendResponseHeaders(statusResposta.get(), resposta.length);
            troca.getResponseBody().write(resposta);
            troca.close();
        });
        servidor.start();

        endpoint = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/v3/smtp/email";
    }

    @AfterEach
    void pararServidor() {
        servidor.stop(0);
    }

    private SendHemoscProperties properties() {
        return new SendHemoscProperties(
                new SendHemoscProperties.Aptidao(60, 90, 4, 3, 16, 69, BigDecimal.valueOf(50)),
                new SendHemoscProperties.Estoque(30, 60),
                new SendHemoscProperties.Notificacao(30, 3, 180, "contato@example.org", "Send Hemosc",
                        "http://localhost:8080"));
    }

    private BrevoEmailAdapter adapter(final String destinatarioTeste) {
        return new BrevoEmailAdapter(properties(), "chave-de-teste", endpoint, destinatarioTeste);
    }

    @Test
    @DisplayName("monta o corpo no formato que a API do Brevo espera")
    void formatoDoCorpo() {
        adapter("").enviar(MENSAGEM);

        final JsonNode corpo = corpos.get(0);
        assertThat(corpo.get("sender").get("email").asText()).isEqualTo("contato@example.org");
        assertThat(corpo.get("sender").get("name").asText()).isEqualTo("Send Hemosc");
        assertThat(corpo.get("to").get(0).get("email").asText()).isEqualTo("doador42@example.org");
        assertThat(corpo.get("subject").asText()).isEqualTo("Estoque O- critico");
        assertThat(corpo.get("htmlContent").asText()).isEqualTo("<p>corpo</p>");
    }

    @Test
    @DisplayName("sem redirecionamento, cada doador recebe no proprio endereco")
    void entregaAoDoador() {
        adapter("").enviar(MENSAGEM);

        assertThat(corpos.get(0).get("to")).hasSize(1);
        assertThat(corpos.get(0).get("to").get(0).get("email").asText()).isEqualTo("doador42@example.org");
    }

    @Test
    @DisplayName("com redirecionamento, o endereco do doador nao vai nos destinatarios")
    void redirecionaQuandoConfigurado() {
        adapter("eu@example.org").enviar(MENSAGEM);

        assertThat(corpos.get(0).get("to").toString())
                .contains("eu@example.org")
                .doesNotContain("doador42@example.org");
    }

    @Test
    @DisplayName("aceita varios destinatarios no redirecionamento")
    void variosDestinatarios() {
        adapter("um@example.org, dois@example.org").enviar(MENSAGEM);

        assertThat(corpos.get(0).get("to")).hasSize(2);
    }

    @Test
    @DisplayName("autentica pelo cabecalho api-key, nao por Bearer")
    void autenticacao() {
        adapter("").enviar(MENSAGEM);

        assertThat(chaves.get(0)).isEqualTo("chave-de-teste");
    }

    @Test
    @DisplayName("erro do servico vira falha de negocio, sem derrubar a rodada")
    void erroViraNegocioException() {
        statusResposta.set(400);

        assertThatThrownBy(() -> adapter("").enviar(MENSAGEM))
                .isInstanceOf(NegocioException.class)
                .hasMessage("notificacao.falha-envio");
    }

    @Test
    @DisplayName("recusa iniciar sem a chave da API, em vez de falhar a cada envio")
    void recusaSemChave() {
        assertThatThrownBy(() -> new BrevoEmailAdapter(properties(), "", endpoint, ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BREVO_API_KEY");
    }
}
