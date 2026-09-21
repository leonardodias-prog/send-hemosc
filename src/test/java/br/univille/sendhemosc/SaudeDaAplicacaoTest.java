package br.univille.sendhemosc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * Guarda a regressao do indicador de saude do servidor de e-mail. Ligado, ele abre uma conexao
 * SMTP autenticada a cada verificacao: a saude da aplicacao passaria a depender do provedor de
 * e-mail, e cada checagem podia levar dez segundos, acima do limite de varias plataformas.
 */
@SpringBootTest
@DisplayName("Saude da aplicacao")
class SaudeDaAplicacaoTest {

    @Autowired
    private ApplicationContext contexto;

    @Test
    @DisplayName("o indicador de saude do servidor de e-mail nao esta registrado")
    void semIndicadorDeEmail() {
        final Map<String, HealthIndicator> indicadores = contexto.getBeansOfType(HealthIndicator.class);

        assertThat(indicadores.keySet())
                .as("indicadores ativos: %s", indicadores.keySet())
                .noneMatch(nome -> nome.toLowerCase().contains("mail"));
    }

    @Test
    @DisplayName("os indicadores essenciais continuam ativos")
    void mantemIndicadoresEssenciais() {
        final Map<String, HealthIndicator> indicadores = contexto.getBeansOfType(HealthIndicator.class);

        assertThat(indicadores).isNotEmpty();
        assertThat(indicadores.keySet())
                .as("indicadores ativos: %s", indicadores.keySet())
                .anyMatch(nome -> nome.toLowerCase().contains("db"));
    }
}
