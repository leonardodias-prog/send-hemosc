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
 * Entrega as mensagens pela API HTTP do Resend.
 *
 * <p>Existe porque varias hospedagens gratuitas bloqueiam a porta SMTP de saida para conter
 * spam, o que faz o envio por SMTP falhar com tempo limite de conexao. A API do Resend responde
 * em HTTPS na porta 443, que nenhuma plataforma bloqueia.</p>
 *
 * <p>Sem um dominio verificado, o Resend so aceita o remetente onboarding@resend.dev e so
 * entrega para o endereco dono da conta. Para este prototipo isso e suficiente, e combina com o
 * redirecionamento de teste, que ja concentra tudo em enderecos conhecidos.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sendhemosc.email.modo", havingValue = "resend")
public class ResendEmailAdapter implements IEmailPort {

    private static final Duration TEMPO_LIMITE = Duration.ofSeconds(10);

    private final RestClient restClient;
    private final SendHemoscProperties properties;
    private final RedirecionamentoDeTeste redirecionamento;

    public ResendEmailAdapter(
            final SendHemoscProperties properties,
            @Value("${sendhemosc.email.resend.api-key:}") final String apiKey,
            @Value("${sendhemosc.email.resend.endpoint:https://api.resend.com/emails}") final String endpoint,
            @Value("${sendhemosc.email.destinatario-teste:}") final String destinatarioTeste) {
        this.properties = properties;
        this.redirecionamento = RedirecionamentoDeTeste.de(destinatarioTeste);

        if (apiKey.isBlank()) {
            throw new IllegalStateException(
                    "sendhemosc.email.modo esta como resend mas RESEND_API_KEY nao foi definida.");
        }

        final SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout((int) TEMPO_LIMITE.toMillis());
        fabrica.setReadTimeout((int) TEMPO_LIMITE.toMillis());

        this.restClient = RestClient.builder()
                .baseUrl(endpoint)
                .requestFactory(fabrica)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();

        if (redirecionamento.isAtivo()) {
            log.warn("[m=init] MODO DE TESTE via Resend: todo e-mail sera redirecionado para {} "
                    + "independentemente do destinatario original", redirecionamento.descricao());
        } else {
            log.warn("[m=init] ENVIO REAL ATIVO via Resend: as mensagens irao para o endereco de cada doador");
        }
    }

    /**
     * Monta o remetente no formato do cabecalho De. O nome exibido vai entre aspas porque
     * parenteses sem aspas sao comentario pela RFC 5322, e o nome configurado contem
     * "(PROTOTIPO)": sem as aspas o endereco poderia ser recusado ou exibido truncado.
     *
     * @return remetente pronto para o cabecalho
     */
    private String remetente() {
        return "\"%s\" <%s>".formatted(
                properties.notificacao().remetenteNome().replace("\"", ""),
                properties.notificacao().remetente());
    }

    @Override
    public void enviar(final MensagemEmail mensagem) {
        final List<String> destinos = redirecionamento.destinosPara(mensagem);

        final Map<String, Object> corpo = Map.of(
                "from", remetente(),
                "to", destinos,
                "subject", redirecionamento.assuntoPara(mensagem),
                "html", mensagem.corpoHtml());

        try {
            restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .toBodilessEntity();

            log.info("[m=enviar] Mensagem de {} entregue ao Resend para {}",
                    mensagem.destinatario(), String.join(", ", destinos));
        } catch (final RestClientException excecao) {
            log.error("[m=enviar] Falha ao entregar ao Resend a mensagem de {}: {}",
                    mensagem.destinatario(), excecao.getMessage());
            throw new NegocioException(NotificacaoErrorsMessage.FALHA_ENVIO, excecao);
        }
    }
}
