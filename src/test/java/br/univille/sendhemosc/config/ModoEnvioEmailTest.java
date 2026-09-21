package br.univille.sendhemosc.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Modo de envio de e-mail")
class ModoEnvioEmailTest {

    @ParameterizedTest(name = "{0} -> envio real: {1}")
    @CsvSource({"log,false", "brevo,true", "LOG,false", "BREVO,true", "BrEvO,true"})
    @DisplayName("reconhece quais modos entregam mensagens de verdade")
    void reconheceEnvioReal(final String configurado, final boolean esperado) {
        assertThat(ModoEnvioEmail.envioReal(configurado)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "desconhecido", "smtp", "resend"})
    @DisplayName("valor nao reconhecido, inclusive modo ja removido, cai no modo seguro")
    void valorDesconhecidoNaoEnvia(final String configurado) {
        assertThat(ModoEnvioEmail.de(configurado)).isEqualTo(ModoEnvioEmail.LOG);
        assertThat(ModoEnvioEmail.envioReal(configurado)).isFalse();
    }

    @Test
    @DisplayName("todo modo novo precisa declarar explicitamente se envia")
    void todosOsModosDeclarados() {
        assertThat(ModoEnvioEmail.values()).hasSize(2);
        assertThat(ModoEnvioEmail.LOG.isEnvioReal()).isFalse();
    }
}
