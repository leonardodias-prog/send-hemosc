package br.univille.sendhemosc.adapter.outbound.email;

import br.univille.sendhemosc.config.SendHemoscProperties;
import br.univille.sendhemosc.domain.dto.MensagemEmail;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.exception.NotificacaoErrorsMessage;
import br.univille.sendhemosc.domain.port.outbound.IEmailPort;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Envio real por SMTP. Em desenvolvimento deve apontar para uma caixa de captura como Mailtrap
 * ou MailHog, nunca para um servidor que entregue a mensagem ao destinatario.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "sendhemosc.email.modo", havingValue = "smtp")
public class SmtpEmailAdapter implements IEmailPort {

    private final JavaMailSender mailSender;
    private final SendHemoscProperties properties;

    @Override
    public void enviar(final MensagemEmail mensagem) {
        try {
            final MimeMessage mime = mailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");

            helper.setFrom(properties.notificacao().remetente(), properties.notificacao().remetenteNome());
            helper.setTo(mensagem.destinatario());
            helper.setSubject(mensagem.assunto());
            helper.setText(mensagem.corpoHtml(), true);

            mailSender.send(mime);
            log.info("[m=enviar] Mensagem enviada para {}", mensagem.destinatario());
        } catch (final MailException | jakarta.mail.MessagingException | UnsupportedEncodingException excecao) {
            log.error("[m=enviar] Falha no envio para {}: {}", mensagem.destinatario(), excecao.getMessage());
            throw new NegocioException(NotificacaoErrorsMessage.FALHA_ENVIO, excecao);
        }
    }
}
