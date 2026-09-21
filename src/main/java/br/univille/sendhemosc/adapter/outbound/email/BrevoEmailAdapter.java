package br.univille.sendhemosc.adapter.outbound.email;

import br.univille.sendhemosc.config.SendHemoscProperties;
import br.univille.sendhemosc.domain.dto.MensagemEmail;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.exception.NotificacaoErrorsMessage;
import br.univille.sendhemosc.domain.port.outbound.IEmailPort;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Entrega as mensagens pela API HTTP do Brevo.
 *
 * <p>Hospedagens gratuitas bloqueiam a porta de saida do SMTP para conter spam, o que faz o
 * envio direto falhar com tempo limite de conexao. A API do Brevo responde em HTTPS na porta
 * 443, que nenhuma plataforma bloqueia.</p>
 *
 * <p>O Brevo aceita remetente verificado de duas formas: um endereco avulso, confirmado por
 * link, ou um dominio inteiro, confirmado por registro DNS. A primeira serve sem dominio
 * proprio, mas a mensagem tende a cair em spam, porque o servidor que entrega nao e o que o
 * dominio do remetente autoriza. Trocar para a segunda e questao de configuracao: basta apontar
 * remetente para uma caixa do dominio verificado.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sendhemosc.email.modo", havingValue = "brevo")
public class BrevoEmailAdapter implements IEmailPort {

    private static final Duration TEMPO_LIMITE = Duration.ofSeconds(10);

    private final RestClient restClient;
    private final SendHemoscProperties properties;
    private final RedirecionamentoDeTeste redirecionamento;

    public BrevoEmailAdapter(
            final SendHemoscProperties properties,
            @Value("${sendhemosc.email.brevo.api-key:}") final String apiKey,
            @Value("${sendhemosc.email.brevo.endpoint:https://api.brevo.com/v3/smtp/email}") final String endpoint,
            @Value("${sendhemosc.email.destinatario-teste:}") final String destinatarioTeste) {
        this.properties = properties;
        this.redirecionamento = RedirecionamentoDeTeste.de(destinatarioTeste);

        if (apiKey.isBlank()) {
            throw new IllegalStateException(
                    "sendhemosc.email.modo esta como brevo mas BREVO_API_KEY nao foi definida.");
        }

        final SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout((int) TEMPO_LIMITE.toMillis());
        fabrica.setReadTimeout((int) TEMPO_LIMITE.toMillis());

        this.restClient = RestClient.builder()
                .baseUrl(endpoint)
                .requestFactory(fabrica)
                .defaultHeader("api-key", apiKey)
                .defaultHeader("accept", MediaType.APPLICATION_JSON_VALUE)
                .build();

        if (redirecionamento.isAtivo()) {
            log.warn("[m=init] Envio pelo Brevo com redirecionamento: toda mensagem ira para {} "
                    + "independentemente do destinatario original", redirecionamento.descricao());
        } else {
            log.warn("[m=init] Envio pelo Brevo SEM redirecionamento: cada doador recebera no proprio endereco");
        }
    }

    @Override
    public void enviar(final MensagemEmail mensagem) {
        final List<String> destinos = redirecionamento.destinosPara(mensagem);

        final Map<String, Object> corpo = Map.of(
                "sender", Map.of(
                        "name", properties.notificacao().remetenteNome(),
                        "email", properties.notificacao().remetente()),
                "to", destinos.stream().map(destino -> Map.of("email", destino)).toList(),
                "subject", redirecionamento.assuntoPara(mensagem),
                "htmlContent", mensagem.corpoHtml());

        try {
            restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .toBodilessEntity();

            log.info("[m=enviar] Mensagem de {} entregue ao Brevo para {}",
                    mensagem.destinatario(), String.join(", ", destinos));
        } catch (final RestClientException excecao) {
            log.error("[m=enviar] Falha ao entregar ao Brevo a mensagem de {}: {}",
                    mensagem.destinatario(), excecao.getMessage());
            throw new NegocioException(NotificacaoErrorsMessage.FALHA_ENVIO, excecao);
        }
    }
}
