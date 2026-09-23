package br.univille.sendhemosc;

import static org.assertj.core.api.Assertions.assertThat;

import br.univille.sendhemosc.domain.exception.DisparoErrorsMessage;
import br.univille.sendhemosc.domain.exception.DoacaoErrorsMessage;
import br.univille.sendhemosc.domain.exception.DoadorErrorsMessage;
import br.univille.sendhemosc.domain.exception.ErrorsMessage;
import br.univille.sendhemosc.domain.exception.EstoqueErrorsMessage;
import br.univille.sendhemosc.domain.exception.NotificacaoErrorsMessage;
import br.univille.sendhemosc.domain.exception.UsuarioErrorsMessage;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.context.support.DelegatingMessageSource;

/**
 * Guarda a regressao do bundle de mensagens: com apenas messages_pt_BR.properties, o Spring Boot
 * nao registrava o MessageSource e toda mensagem — de erro da API e de validacao de formulario —
 * era devolvida como a chave crua.
 */
@SpringBootTest
@DisplayName("Bundle de mensagens")
class MensagensTest {

    private static final String SEM_TRADUCAO = "__sem_traducao__";

    @Autowired
    private MessageSource messageSource;

    @Test
    @DisplayName("o MessageSource registrado nao e o delegate vazio do fallback")
    void messageSourceRegistrado() {
        assertThat(messageSource).isNotInstanceOf(DelegatingMessageSource.class);
    }

    @Test
    @DisplayName("todo codigo de erro do catalogo tem mensagem traduzida")
    void catalogosDeErroTraduzidos() {
        final Stream<ErrorsMessage> catalogos = Stream.of(
                        DoadorErrorsMessage.values(),
                        EstoqueErrorsMessage.values(),
                        NotificacaoErrorsMessage.values(),
                        DoacaoErrorsMessage.values(),
                        UsuarioErrorsMessage.values(),
                        DisparoErrorsMessage.values())
                .flatMap(Stream::of);

        catalogos.forEach(erro -> {
            final String mensagem = messageSource.getMessage(
                    erro.getChaveMensagem(), null, SEM_TRADUCAO, Locale.of("pt", "BR"));

            assertThat(mensagem)
                    .as("chave %s (codigo %s)", erro.getChaveMensagem(), erro.getCodigo())
                    .isNotEqualTo(SEM_TRADUCAO)
                    .isNotEqualTo(erro.getChaveMensagem())
                    .isNotBlank();
        });
    }

    @Test
    @DisplayName("as mensagens de validacao do cadastro de doador existem")
    void mensagensDeValidacaoExistem() {
        final List<String> chaves = List.of(
                "doador.nome.obrigatorio",
                "doador.email.obrigatorio",
                "doador.email.invalido",
                "doador.tipo-sanguineo.obrigatorio",
                "doador.sexo.obrigatorio",
                "doador.data-nascimento.obrigatoria",
                "doador.data-nascimento.invalida");

        chaves.forEach(chave -> assertThat(
                messageSource.getMessage(chave, null, SEM_TRADUCAO, Locale.of("pt", "BR")))
                .as("chave %s", chave)
                .isNotEqualTo(SEM_TRADUCAO));
    }
}
