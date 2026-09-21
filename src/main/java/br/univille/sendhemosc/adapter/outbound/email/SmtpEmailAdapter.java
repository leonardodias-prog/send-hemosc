package br.univille.sendhemosc.adapter.outbound.email;

import br.univille.sendhemosc.config.SendHemoscProperties;
import br.univille.sendhemosc.domain.dto.MensagemEmail;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.exception.NotificacaoErrorsMessage;
import br.univille.sendhemosc.domain.port.outbound.IEmailPort;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
@ConditionalOnProperty(name = "sendhemosc.email.modo", havingValue = "smtp")
public class SmtpEmailAdapter implements IEmailPort {

    private final JavaMailSender mailSender;
    private final SendHemoscProperties properties;
    private final RedirecionamentoDeTeste redirecionamento;

    public SmtpEmailAdapter(final JavaMailSender mailSender,
                            final SendHemoscProperties properties,
                            @Value("${sendhemosc.email.destinatario-teste:}") final String destinatarioTeste) {
        this.mailSender = mailSender;
        this.properties = properties;
        this.redirecionamento = RedirecionamentoDeTeste.de(destinatarioTeste);

        if (redirecionamento.isAtivo()) {
            log.warn("[m=init] MODO DE TESTE: todo e-mail sera redirecionado para {} "
                    + "independentemente do destinatario original", redirecionamento.descricao());
        } else {
            log.warn("[m=init] ENVIO REAL ATIVO: as mensagens irao para o endereco de cada doador. "
                    + "Defina sendhemosc.email.destinatario-teste para redirecionar tudo a enderecos conhecidos");
        }
    }

    @Override
    public void enviar(final MensagemEmail mensagem) {
        final List<String> destinos = redirecionamento.destinosPara(mensagem);
        final String assunto = redirecionamento.assuntoPara(mensagem);

        try {
            final MimeMessage mime = mailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");

            helper.setFrom(properties.notificacao().remetente(), properties.notificacao().remetenteNome());
            helper.setTo(destinos.toArray(String[]::new));
            helper.setSubject(assunto);
            helper.setText(mensagem.corpoHtml(), true);

            mailSender.send(mime);

            if (redirecionamento.isAtivo()) {
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
