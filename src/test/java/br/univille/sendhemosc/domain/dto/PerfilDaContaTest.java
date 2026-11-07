package br.univille.sendhemosc.domain.dto;

import static org.assertj.core.api.Assertions.assertThat;

import br.univille.sendhemosc.domain.enums.PerfilUsuario;
import br.univille.sendhemosc.domain.enums.SituacaoUsuario;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Inicial do nome no lugar da foto")
class PerfilDaContaTest {

    private static String inicialDe(final String nome) {
        final UsuarioResumo usuario = new UsuarioResumo(1L, nome, "a@example.org", PerfilUsuario.OPERADOR,
                SituacaoUsuario.ATIVO, LocalDateTime.now(), null);

        return new PerfilDaConta(usuario, false).inicial();
    }

    @Test
    @DisplayName("e a primeira letra do nome, em maiuscula")
    void primeiraLetraEmMaiuscula() {
        assertThat(inicialDe("rafaela souza")).isEqualTo("R");
        assertThat(inicialDe("Rafaela Souza")).isEqualTo("R");
    }

    @Test
    @DisplayName("letra acentuada continua inteira")
    void letraAcentuada() {
        assertThat(inicialDe("érica")).isEqualTo("É");
        assertThat(inicialDe("Ângela")).isEqualTo("Â");
    }

    @Test
    @DisplayName("espaco antes do nome nao vira a inicial")
    void ignoraEspacoNoComeco() {
        assertThat(inicialDe("   maria")).isEqualTo("M");
    }

    @Test
    @DisplayName("caractere fora do plano basico nao e cortado ao meio")
    void caractereForaDoPlanoBasico() {
        assertThat(inicialDe("😀 sorriso")).isEqualTo("😀");
    }

    @Test
    @DisplayName("nome vazio nao quebra a tela")
    void nomeVazio() {
        assertThat(inicialDe("   ")).isEqualTo("?");
        assertThat(inicialDe(null)).isEqualTo("?");
    }
}
