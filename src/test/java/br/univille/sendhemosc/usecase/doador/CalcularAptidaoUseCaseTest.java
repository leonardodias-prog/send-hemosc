package br.univille.sendhemosc.usecase.doador;

import static org.assertj.core.api.Assertions.assertThat;

import br.univille.sendhemosc.config.SendHemoscProperties;
import br.univille.sendhemosc.domain.dto.AptidaoDoador;
import br.univille.sendhemosc.domain.enums.Sexo;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Calculo de aptidao do doador")
class CalcularAptidaoUseCaseTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 9, 18);

    private CalcularAptidaoUseCase useCase;

    @BeforeEach
    void preparar() {
        final var aptidao = new SendHemoscProperties.Aptidao(
                60, 90, 4, 3, 16, 69, BigDecimal.valueOf(50));
        final var estoque = new SendHemoscProperties.Estoque(30, 60);
        final var notificacao = new SendHemoscProperties.Notificacao(30, 3, "teste@example.org", "Teste", "http://localhost:8080");

        useCase = new CalcularAptidaoUseCase(new SendHemoscProperties(aptidao, estoque, notificacao));
    }

    private CalcularAptidaoUseCase.DadosAptidao dados(final Sexo sexo, final LocalDate ultimaDoacao,
                                                      final long doacoesJanela) {
        return new CalcularAptidaoUseCase.DadosAptidao(
                sexo, HOJE.minusYears(30), BigDecimal.valueOf(72), ultimaDoacao, doacoesJanela, HOJE);
    }

    @Nested
    @DisplayName("intervalo entre doacoes")
    class Intervalo {

        @Test
        @DisplayName("homem com 60 dias completos desde a ultima doacao esta apto")
        void homemComIntervaloCumprido() {
            final AptidaoDoador resultado = useCase.execute(dados(Sexo.MASCULINO, HOJE.minusDays(60), 1));

            assertThat(resultado.apto()).isTrue();
            assertThat(resultado.motivosInaptidao()).isEmpty();
        }

        @Test
        @DisplayName("homem com 59 dias ainda esta inapto e recebe a data em que volta a poder doar")
        void homemComIntervaloIncompleto() {
            final AptidaoDoador resultado = useCase.execute(dados(Sexo.MASCULINO, HOJE.minusDays(59), 1));

            assertThat(resultado.apto()).isFalse();
            assertThat(resultado.motivosInaptidao()).containsExactly("doador.inapto.intervalo");
            assertThat(resultado.proximaDataApta()).isEqualTo(HOJE.plusDays(1));
        }

        @Test
        @DisplayName("mulher exige 90 dias, entao 60 dias ainda e inapto")
        void mulherExigeIntervaloMaior() {
            final AptidaoDoador resultado = useCase.execute(dados(Sexo.FEMININO, HOJE.minusDays(60), 1));

            assertThat(resultado.apto()).isFalse();
            assertThat(resultado.motivosInaptidao()).contains("doador.inapto.intervalo");
        }

        @Test
        @DisplayName("quem nunca doou esta apto a partir de hoje")
        void primeiraDoacao() {
            final AptidaoDoador resultado = useCase.execute(dados(Sexo.FEMININO, null, 0));

            assertThat(resultado.apto()).isTrue();
            assertThat(resultado.proximaDataApta()).isEqualTo(HOJE);
        }
    }

    @Nested
    @DisplayName("limite anual de doacoes")
    class LimiteAnual {

        @Test
        @DisplayName("homem na quarta doacao em doze meses fica inapto")
        void homemNoLimite() {
            final AptidaoDoador resultado = useCase.execute(dados(Sexo.MASCULINO, HOJE.minusDays(200), 4));

            assertThat(resultado.apto()).isFalse();
            assertThat(resultado.motivosInaptidao()).contains("doador.inapto.limite-anual");
        }

        @Test
        @DisplayName("mulher na terceira doacao em doze meses fica inapta")
        void mulherNoLimite() {
            final AptidaoDoador resultado = useCase.execute(dados(Sexo.FEMININO, HOJE.minusDays(200), 3));

            assertThat(resultado.apto()).isFalse();
            assertThat(resultado.motivosInaptidao()).contains("doador.inapto.limite-anual");
        }
    }

    @Nested
    @DisplayName("faixa etaria e peso")
    class IdadeEPeso {

        @Test
        @DisplayName("abaixo da idade minima fica inapto")
        void menorDeIdadeMinima() {
            final var entrada = new CalcularAptidaoUseCase.DadosAptidao(
                    Sexo.MASCULINO, HOJE.minusYears(15), BigDecimal.valueOf(70), null, 0, HOJE);

            assertThat(useCase.execute(entrada).motivosInaptidao()).contains("doador.inapto.idade");
        }

        @Test
        @DisplayName("acima da idade maxima fica inapto")
        void acimaDaIdadeMaxima() {
            final var entrada = new CalcularAptidaoUseCase.DadosAptidao(
                    Sexo.MASCULINO, HOJE.minusYears(70), BigDecimal.valueOf(70), null, 0, HOJE);

            assertThat(useCase.execute(entrada).motivosInaptidao()).contains("doador.inapto.idade");
        }

        @Test
        @DisplayName("abaixo do peso minimo fica inapto")
        void abaixoDoPeso() {
            final var entrada = new CalcularAptidaoUseCase.DadosAptidao(
                    Sexo.FEMININO, HOJE.minusYears(25), BigDecimal.valueOf(49.9), null, 0, HOJE);

            assertThat(useCase.execute(entrada).motivosInaptidao()).contains("doador.inapto.peso");
        }

        @Test
        @DisplayName("peso nao informado nao bloqueia a aptidao")
        void pesoNulo() {
            final var entrada = new CalcularAptidaoUseCase.DadosAptidao(
                    Sexo.FEMININO, HOJE.minusYears(25), null, null, 0, HOJE);

            assertThat(useCase.execute(entrada).apto()).isTrue();
        }

        @Test
        @DisplayName("varios impedimentos sao reportados juntos")
        void multiplosMotivos() {
            final var entrada = new CalcularAptidaoUseCase.DadosAptidao(
                    Sexo.MASCULINO, HOJE.minusYears(15), BigDecimal.valueOf(40), HOJE.minusDays(5), 9, HOJE);

            assertThat(useCase.execute(entrada).motivosInaptidao()).hasSize(4);
        }
    }
}
