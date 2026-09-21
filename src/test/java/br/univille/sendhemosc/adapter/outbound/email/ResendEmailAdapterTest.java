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
 * Exercita o adapter contra um servidor HTTP local, de modo a verificar o que e realmente
 * enviado ao Resend: o formato do corpo, o cabecalho de autenticacao e, principalmente, que o
 * redirecionamento de teste impede o endereco do doador de chegar na requisicao.
 */
@DisplayName("Envio pela API do Resend")
class ResendEmailAdapterTest {

    private static final MensagemEmail MENSAGEM =
            new MensagemEmail("doador42@example.org", "Estoque O- critico", "<p>corpo</p>");

    private static final ObjectMapper JSON = new ObjectMapper();

    private HttpServer servidor;
    private String endpoint;
    private final List<JsonNode> corpos = new ArrayList<>();
    private final List<String> autorizacoes = new ArrayList<>();
    private final AtomicInteger statusResposta = new AtomicInteger(200);

    @BeforeEach
    void subirServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/emails", troca -> {
            autorizacoes.add(troca.getRequestHeaders().getFirst("Authorization"));
            corpos.add(JSON.readTree(troca.getRequestBody().readAllBytes()));

            final byte[] resposta = "{\"id\":\"msg_1\"}".getBytes(StandardCharsets.UTF_8);
            troca.sendResponseHeaders(statusResposta.get(), resposta.length);
            troca.getResponseBody().write(resposta);
            troca.close();
        });
        servidor.start();

        endpoint = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/emails";
    }

    @AfterEach
    void pararServidor() {
        servidor.stop(0);
    }

    private SendHemoscProperties properties() {
        return new SendHemoscProperties(
                new SendHemoscProperties.Aptidao(60, 90, 4, 3, 16, 69, BigDecimal.valueOf(50)),
                new SendHemoscProperties.Estoque(30, 60),
                new SendHemoscProperties.Notificacao(30, "onboarding@resend.dev", "Send Hemosc",
                        "http://localhost:8080"));
    }

    private ResendEmailAdapter adapter(final String destinatarioTeste) {
        return new ResendEmailAdapter(properties(), "re_chave_de_teste", endpoint, destinatarioTeste);
    }

    @Test
    @DisplayName("com destinatario de teste, o endereco do doador NAO vai na requisicao")
    void redirecionaEProtegeODoador() {
        adapter("eu@example.org").enviar(MENSAGEM);

        final JsonNode corpo = corpos.get(0);
        assertThat(corpo.get("to")).hasSize(1);
        assertThat(corpo.get("to").get(0).asText()).isEqualTo("eu@example.org");
        assertThat(corpo.toString()).doesNotContain("doador42@example.org.\"");
        assertThat(corpo.get("to").toString()).doesNotContain("doador42@example.org");
    }

    @Test
    @DisplayName("aceita varios destinatarios de teste")
    void variosDestinatarios() {
        adapter("um@example.org, dois@example.org").enviar(MENSAGEM);

        assertThat(corpos.get(0).get("to")).hasSize(2);
    }

    @Test
    @DisplayName("sem destinatario de teste, envia para o doador")
    void semRedirecionamento() {
        adapter("").enviar(MENSAGEM);

        assertThat(corpos.get(0).get("to").get(0).asText()).isEqualTo("doador42@example.org");
    }

    @Test
    @DisplayName("o assunto preserva o destinatario original para conferencia")
    void assuntoComDestinatarioOriginal() {
        adapter("eu@example.org").enviar(MENSAGEM);

        assertThat(corpos.get(0).get("subject").asText())
                .startsWith("[TESTE -> doador42@example.org]")
                .contains("Estoque O- critico");
    }

    @Test
    @DisplayName("envia o corpo como HTML e o remetente no formato aceito pelo Resend")
    void corpoERemetente() {
        adapter("eu@example.org").enviar(MENSAGEM);

        final JsonNode corpo = corpos.get(0);
        assertThat(corpo.get("html").asText()).isEqualTo("<p>corpo</p>");
        assertThat(corpo.get("from").asText()).isEqualTo("\"Send Hemosc\" <onboarding@resend.dev>");
    }

    @Test
    @DisplayName("nome de remetente com parenteses sai entre aspas, conforme a RFC 5322")
    void remetenteComParentesesFicaEntreAspas() {
        final SendHemoscProperties comParenteses = new SendHemoscProperties(
                properties().aptidao(), properties().estoque(),
                new SendHemoscProperties.Notificacao(30, "onboarding@resend.dev",
                        "Send Hemosc (PROTOTIPO)", "http://localhost:8080"));

        new ResendEmailAdapter(comParenteses, "re_k", endpoint, "eu@example.org").enviar(MENSAGEM);

        assertThat(corpos.get(0).get("from").asText())
                .isEqualTo("\"Send Hemosc (PROTOTIPO)\" <onboarding@resend.dev>");
    }

    @Test
    @DisplayName("autentica com a chave da API no cabecalho")
    void enviaChaveDeApi() {
        adapter("eu@example.org").enviar(MENSAGEM);

        assertThat(autorizacoes.get(0)).isEqualTo("Bearer re_chave_de_teste");
    }

    @Test
    @DisplayName("erro do servico vira falha de negocio, sem derrubar a rodada")
    void erroDoServicoViraNegocioException() {
        statusResposta.set(422);

        assertThatThrownBy(() -> adapter("eu@example.org").enviar(MENSAGEM))
                .isInstanceOf(NegocioException.class)
                .hasMessage("notificacao.falha-envio");
    }

    @Test
    @DisplayName("recusa iniciar sem a chave da API, em vez de falhar a cada envio")
    void recusaSemChave() {
        assertThatThrownBy(() -> new ResendEmailAdapter(properties(), "", endpoint, "eu@example.org"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RESEND_API_KEY");
    }
}
