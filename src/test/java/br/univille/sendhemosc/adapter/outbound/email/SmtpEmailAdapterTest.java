package br.univille.sendhemosc.adapter.outbound.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.univille.sendhemosc.config.SendHemoscProperties;
import br.univille.sendhemosc.domain.dto.MensagemEmail;
import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Cobre a trava de seguranca do envio real: com destinatario-teste preenchido, nenhuma mensagem
 * pode chegar ao endereco do doador. Sem essa trava, ligar o modo smtp com a massa ficticia
 * carregada dispararia para a base inteira.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Envio por SMTP")
class SmtpEmailAdapterTest {

    private static final MensagemEmail MENSAGEM =
            new MensagemEmail("doador42@example.org", "Estoque O- critico", "<p>corpo</p>");

    @Mock
    private JavaMailSender mailSender;

    private SendHemoscProperties properties;

    @BeforeEach
    void preparar() {
        properties = new SendHemoscProperties(
                new SendHemoscProperties.Aptidao(60, 90, 4, 3, 16, 69, BigDecimal.valueOf(50)),
                new SendHemoscProperties.Estoque(30, 60),
                new SendHemoscProperties.Notificacao(30, "remetente@example.org", "Send Hemosc",
                        "http://localhost:8080"));
    }

    private MimeMessage capturarEnvio(final String destinatarioTeste) throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((jakarta.mail.Session) null));

        new SmtpEmailAdapter(mailSender, properties, destinatarioTeste).enviar(MENSAGEM);

        final ArgumentCaptor<MimeMessage> enviada = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(enviada.capture());
        return enviada.getValue();
    }

    @Test
    @DisplayName("com destinatario de teste, a mensagem NAO chega ao doador")
    void redirecionaParaOEnderecoDeTeste() throws Exception {
        final MimeMessage enviada = capturarEnvio("eu@example.org");

        assertThat(enviada.getRecipients(Message.RecipientType.TO))
                .hasSize(1)
                .allMatch(destino -> destino.toString().equals("eu@example.org"));
    }

    @Test
    @DisplayName("o assunto preserva o destinatario original para conferencia")
    void assuntoMostraDestinatarioOriginal() throws Exception {
        final MimeMessage enviada = capturarEnvio("eu@example.org");

        assertThat(enviada.getSubject())
                .startsWith("[TESTE -> doador42@example.org]")
                .contains("Estoque O- critico");
    }

    @Test
    @DisplayName("sem destinatario de teste, a mensagem vai para o doador")
    void semRedirecionamentoVaiParaODoador() throws Exception {
        final MimeMessage enviada = capturarEnvio("");

        assertThat(enviada.getRecipients(Message.RecipientType.TO))
                .hasSize(1)
                .allMatch(destino -> destino.toString().equals("doador42@example.org"));
        assertThat(enviada.getSubject()).isEqualTo("Estoque O- critico");
    }

    @Test
    @DisplayName("aceita varios destinatarios separados por virgula")
    void aceitaVariosDestinatarios() throws Exception {
        final MimeMessage enviada = capturarEnvio("um@example.org,dois@example.org,tres@example.org");

        assertThat(enviada.getRecipients(Message.RecipientType.TO))
                .hasSize(3)
                .extracting(Object::toString)
                .containsExactlyInAnyOrder("um@example.org", "dois@example.org", "tres@example.org");
    }

    @Test
    @DisplayName("ignora espacos e entradas vazias na lista de destinatarios")
    void limpaListaDeDestinatarios() throws Exception {
        final MimeMessage enviada = capturarEnvio("  um@example.org , , dois@example.org ,");

        assertThat(enviada.getRecipients(Message.RecipientType.TO))
                .hasSize(2)
                .extracting(Object::toString)
                .containsExactlyInAnyOrder("um@example.org", "dois@example.org");
    }

    @Test
    @DisplayName("com varios destinatarios o doador original continua fora da lista")
    void doadorNuncaEntraNaLista() throws Exception {
        final MimeMessage enviada = capturarEnvio("um@example.org,dois@example.org");

        assertThat(enviada.getRecipients(Message.RecipientType.TO))
                .extracting(Object::toString)
                .doesNotContain("doador42@example.org");
    }

    @Test
    @DisplayName("espaco em branco no destinatario de teste conta como nao configurado")
    void brancoNaoEConsideradoRedirecionamento() throws Exception {
        final MimeMessage enviada = capturarEnvio("   ");

        assertThat(enviada.getRecipients(Message.RecipientType.TO))
                .allMatch(destino -> destino.toString().equals("doador42@example.org"));
    }

    @Test
    @DisplayName("o remetente configurado e aplicado")
    void aplicaRemetente() throws Exception {
        final MimeMessage enviada = capturarEnvio("eu@example.org");

        assertThat(enviada.getFrom()).hasSize(1);
        assertThat(enviada.getFrom()[0].toString()).contains("remetente@example.org");
    }

    @Test
    @DisplayName("o corpo vai como HTML")
    void corpoHtml() throws Exception {
        final MimeMessage enviada = capturarEnvio("eu@example.org");

        // JavaMailSenderImpl chama saveChanges antes de transmitir; com o sender mockado
        // isso nao acontece, e sem essa chamada o cabecalho de tipo fica no valor padrao.
        enviada.saveChanges();

        assertThat(enviada.getContentType()).contains("text/html");
        assertThat(enviada.getContent().toString()).contains("<p>corpo</p>");
    }

    @Test
    @DisplayName("aceita mensagem sem lancar quando o envio ocorre normalmente")
    void envioNormalNaoLancaExcecao() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((jakarta.mail.Session) null));

        final SmtpEmailAdapter adapter = new SmtpEmailAdapter(mailSender, properties, "eu@example.org");

        adapter.enviar(MENSAGEM);

        verify(mailSender).send(any(MimeMessage.class));
    }
}
