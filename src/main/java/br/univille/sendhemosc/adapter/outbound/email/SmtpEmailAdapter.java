package br.univille.sendhemosc.adapter.outbound.email;

import br.univille.sendhemosc.config.SendHemoscProperties;
import br.univille.sendhemosc.domain.dto.MensagemEmail;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.exception.NotificacaoErrorsMessage;
import br.univille.sendhemosc.domain.port.outbound.IEmailPort;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
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
    private final String destinatarioTeste;

    public SmtpEmailAdapter(final JavaMailSender mailSender,
                            final SendHemoscProperties properties,
                            @Value("${sendhemosc.email.destinatario-teste:}") final String destinatarioTeste) {
        this.mailSender = mailSender;
        this.properties = properties;
        this.destinatarioTeste = destinatarioTeste;

        if (StringUtils.hasText(destinatarioTeste)) {
            log.warn("[m=init] MODO DE TESTE: todo e-mail sera redirecionado para {} "
                    + "independentemente do destinatario original", destinatarioTeste);
        } else {
            log.warn("[m=init] ENVIO REAL ATIVO: as mensagens irao para o endereco de cada doador. "
                    + "Defina sendhemosc.email.destinatario-teste para redirecionar tudo a um unico endereco");
        }
    }

    @Override
    public void enviar(final MensagemEmail mensagem) {
        final boolean redirecionado = StringUtils.hasText(destinatarioTeste);
        final String destino = redirecionado ? destinatarioTeste : mensagem.destinatario();
        final String assunto = redirecionado
                ? "[TESTE -> %s] %s".formatted(mensagem.destinatario(), mensagem.assunto())
                : mensagem.assunto();

        try {
            final MimeMessage mime = mailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");

            helper.setFrom(properties.notificacao().remetente(), properties.notificacao().remetenteNome());
            helper.setTo(destino);
            helper.setSubject(assunto);
            helper.setText(mensagem.corpoHtml(), true);

            mailSender.send(mime);

            if (redirecionado) {
                log.info("[m=enviar] Mensagem de {} redirecionada para {}", mensagem.destinatario(), destino);
            } else {
                log.info("[m=enviar] Mensagem enviada para {}", destino);
            }
        } catch (final MailException | MessagingException | UnsupportedEncodingException excecao) {
            log.error("[m=enviar] Falha no envio para {}: {}", destino, excecao.getMessage());
            throw new NegocioException(NotificacaoErrorsMessage.FALHA_ENVIO, excecao);
        }
    }
}
