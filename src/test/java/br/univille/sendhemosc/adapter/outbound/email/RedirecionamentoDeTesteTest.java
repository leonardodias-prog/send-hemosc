package br.univille.sendhemosc.adapter.outbound.email;

import static org.assertj.core.api.Assertions.assertThat;

import br.univille.sendhemosc.domain.dto.MensagemEmail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Trava de redirecionamento de teste")
class RedirecionamentoDeTesteTest {

    private static final MensagemEmail MENSAGEM =
            new MensagemEmail("doador@example.org", "Assunto", "<p>corpo</p>");

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", ",", " , , "})
    @DisplayName("sem endereco util a trava fica inativa e a mensagem segue para o doador")
    void inativaSemEndereco(final String configurado) {
        final RedirecionamentoDeTeste trava = RedirecionamentoDeTeste.de(configurado);

        assertThat(trava.isAtivo()).isFalse();
        assertThat(trava.destinosPara(MENSAGEM)).containsExactly("doador@example.org");
        assertThat(trava.assuntoPara(MENSAGEM)).isEqualTo("Assunto");
    }

    @Test
    @DisplayName("com a trava ativa o doador nunca aparece nos destinos")
    void doadorNuncaRecebe() {
        final RedirecionamentoDeTeste trava = RedirecionamentoDeTeste.de("eu@example.org");

        assertThat(trava.destinosPara(MENSAGEM))
                .containsExactly("eu@example.org")
                .doesNotContain("doador@example.org");
    }

    @Test
    @DisplayName("separa varios enderecos e limpa espacos e entradas vazias")
    void separaELimpa() {
        final RedirecionamentoDeTeste trava = RedirecionamentoDeTeste.de("  um@a.com , ,dois@b.com, ");

        assertThat(trava.getDestinatarios()).containsExactly("um@a.com", "dois@b.com");
        assertThat(trava.descricao()).isEqualTo("um@a.com, dois@b.com");
    }

    @Test
    @DisplayName("o assunto guarda o destinatario original para conferencia")
    void assuntoPreservaOriginal() {
        assertThat(RedirecionamentoDeTeste.de("eu@a.com").assuntoPara(MENSAGEM))
                .isEqualTo("[TESTE -> doador@example.org] Assunto");
    }
}
