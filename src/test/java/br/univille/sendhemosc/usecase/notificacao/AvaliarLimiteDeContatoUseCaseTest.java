package br.univille.sendhemosc.usecase.notificacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.univille.sendhemosc.config.SendHemoscProperties;
import br.univille.sendhemosc.domain.dto.LimiteDeContato;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Limites de contato com intervalo de 30 dias e teto de 3 convocacoes sem resposta.
 */
@DisplayName("Limite de contato por pessoa")
class AvaliarLimiteDeContatoUseCaseTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 9, 23);

    private AvaliarLimiteDeContatoUseCase useCase;

    @BeforeEach
    void preparar() {
        useCase = new AvaliarLimiteDeContatoUseCase(new SendHemoscProperties(
                new SendHemoscProperties.Aptidao(60, 90, 4, 3, 16, 69, BigDecimal.valueOf(50)),
                new SendHemoscProperties.Estoque(30, 60),
                new SendHemoscProperties.Notificacao(30, 3, 180, "teste@example.org", "Teste", "http://localhost:8080")));
    }

    private LocalDateTime diasAtras(final int dias) {
        return HOJE.minusDays(dias).atTime(9, 30);
    }

    @Test
    @DisplayName("quem nunca foi convocado esta liberado")
    void nuncaConvocado() {
        assertThat(useCase.execute(0, null, HOJE).liberado()).isTrue();
    }

    @Nested
    @DisplayName("intervalo entre convocacoes")
    class Intervalo {

        @Test
        @DisplayName("convocado ha 5 dias fica retido, e sabe-se quando volta")
        void recente() {
            final LimiteDeContato limite = useCase.execute(1, diasAtras(5), HOJE);

            assertThat(limite.liberado()).isFalse();
            assertThat(limite.tetoAtingido()).isFalse();
            assertThat(limite.liberadoEm()).isEqualTo(HOJE.plusDays(25));
        }

        @Test
        @DisplayName("no trigesimo dia volta a poder ser convocado")
        void exatamenteNoIntervalo() {
            assertThat(useCase.execute(1, diasAtras(30), HOJE).liberado()).isTrue();
        }

        @Test
        @DisplayName("um dia antes do fim do intervalo ainda esta retido")
        void umDiaAntes() {
            assertThat(useCase.execute(1, diasAtras(29), HOJE).liberado()).isFalse();
        }

        @Test
        @DisplayName("convocado hoje nao e convocado de novo hoje: e o que impede a rodada diaria de insistir")
        void mesmoDia() {
            assertThat(useCase.execute(1, HOJE.atTime(8, 0), HOJE).liberado()).isFalse();
        }
    }

    @Nested
    @DisplayName("teto de convocacoes sem resposta")
    class Teto {

        @Test
        @DisplayName("abaixo do teto, com intervalo cumprido, esta liberado")
        void abaixo() {
            assertThat(useCase.execute(2, diasAtras(40), HOJE).liberado()).isTrue();
        }

        @Test
        @DisplayName("no teto fica retido sem data de volta: so uma doacao desfaz")
        void noTeto() {
            final LimiteDeContato limite = useCase.execute(3, diasAtras(200), HOJE);

            assertThat(limite.liberado()).isFalse();
            assertThat(limite.tetoAtingido()).isTrue();
            assertThat(limite.liberadoEm()).isNull();
        }

        @Test
        @DisplayName("o teto prevalece sobre o intervalo: nao volta a ser chamado so porque o tempo passou")
        void tetoAntesDoIntervalo() {
            assertThat(useCase.execute(3, diasAtras(5), HOJE).tetoAtingido()).isTrue();
        }

        @Test
        @DisplayName("depois de uma doacao a contagem zera e a pessoa volta a estar liberada")
        void doacaoZera() {
            // Registrar a doacao fecha as convocacoes pendentes: sobram zero sem resposta.
            assertThat(useCase.execute(0, diasAtras(60), HOJE).liberado()).isTrue();
        }
    }
}
