package br.univille.sendhemosc.domain.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Mascara de e-mail para log")
class MascaraDeEmailTest {

    @Test
    @DisplayName("mantem a primeira letra e o dominio")
    void mascaraUmEndereco() {
        assertThat(MascaraDeEmail.mascarar("maria.silva@exemplo.com")).isEqualTo("m***@exemplo.com");
    }

    @Test
    @DisplayName("valor nulo, vazio ou sem arroba passa como veio")
    void naoMexeNoQueNaoEEmail() {
        assertThat(MascaraDeEmail.mascarar(null)).isNull();
        assertThat(MascaraDeEmail.mascarar("")).isEmpty();
        assertThat(MascaraDeEmail.mascarar("sistema")).isEqualTo("sistema");
    }

    @Test
    @DisplayName("mascara todo endereco que aparece dentro de um texto")
    void mascaraEnderecosEmTextoLivre() {
        final String texto = "Maria (maria@exemplo.com) foi aprovada por admin@hemosc.org.br";

        assertThat(MascaraDeEmail.mascararEmTexto(texto))
                .isEqualTo("Maria (m***@exemplo.com) foi aprovada por a***@hemosc.org.br");
    }

    @Test
    @DisplayName("texto sem endereco fica igual")
    void textoSemEndereco() {
        assertThat(MascaraDeEmail.mascararEmTexto("Estoque A+ atualizado")).isEqualTo("Estoque A+ atualizado");
        assertThat(MascaraDeEmail.mascararEmTexto(null)).isNull();
    }
}
