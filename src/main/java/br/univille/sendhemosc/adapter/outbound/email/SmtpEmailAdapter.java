package br.univille.sendhemosc.adapter.outbound.email;

import br.univille.sendhemosc.config.SendHemoscProperties;
import br.univille.sendhemosc.domain.dto.MensagemEmail;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.exception.NotificacaoErrorsMessage;
import br.univille.sendhemosc.domain.port.outbound.IEmailPort;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Envio real por SMTP. Em desenvolvimento deve apontar para uma caixa de captura como Mailtrap
 * ou MailHog, nunca para um servidor que entregue a mensagem ao destinatario.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sendhemosc.email.modo", havingValue = "smtp")
public class SmtpEmailAdapter implements IEmailPort {

    private final JavaMailSender mailSender;
    private final SendHemoscProperties properties;
    private final String[] destinatariosTeste;

    public SmtpEmailAdapter(final JavaMailSender mailSender,
                            final SendHemoscProperties properties,
                            @Value("${sendhemosc.email.destinatario-teste:}") final String destinatarioTeste) {
        this.mailSender = mailSender;
        this.properties = properties;
        this.destinatariosTeste = separarEnderecos(destinatarioTeste);

        if (destinatariosTeste.length > 0) {
            log.warn("[m=init] MODO DE TESTE: todo e-mail sera redirecionado para {} "
                    + "independentemente do destinatario original", String.join(", ", destinatariosTeste));
        } else {
            log.warn("[m=init] ENVIO REAL ATIVO: as mensagens irao para o endereco de cada doador. "
                    + "Defina sendhemosc.email.destinatario-teste para redirecionar tudo a enderecos conhecidos");
        }
    }

    /**
     * Aceita um ou varios enderecos separados por virgula, de modo que a equipe inteira
     * possa acompanhar um teste de envio ao mesmo tempo.
     *
     * @param configurado valor bruto da propriedade
     * @return enderecos limpos, vazio quando nada foi configurado
     */
    private static String[] separarEnderecos(final String configurado) {
        if (!StringUtils.hasText(configurado)) {
            return new String[0];
        }

        return Arrays.stream(configurado.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toArray(String[]::new);
    }

    @Override
    public void enviar(final MensagemEmail mensagem) {
        final boolean redirecionado = destinatariosTeste.length > 0;
        final String[] destinos = redirecionado ? destinatariosTeste : new String[] {mensagem.destinatario()};
        final String assunto = redirecionado
                ? "[TESTE -> %s] %s".formatted(mensagem.destinatario(), mensagem.assunto())
                : mensagem.assunto();

        try {
            final MimeMessage mime = mailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");

            helper.setFrom(properties.notificacao().remetente(), properties.notificacao().remetenteNome());
            helper.setTo(destinos);
            helper.setSubject(assunto);
            helper.setText(mensagem.corpoHtml(), true);

            mailSender.send(mime);

            if (redirecionado) {
                log.info("[m=enviar] Mensagem de {} redirecionada para {}",
                        mensagem.destinatario(), String.join(", ", destinos));
            } else {
                log.info("[m=enviar] Mensagem enviada para {}", mensagem.destinatario());
            }
        } catch (final MailException | MessagingException | UnsupportedEncodingException excecao) {
            log.error("[m=enviar] Falha no envio de {}: {}", mensagem.destinatario(), excecao.getMessage());
            throw new NegocioException(NotificacaoErrorsMessage.FALHA_ENVIO, excecao);
        }
    }
}
