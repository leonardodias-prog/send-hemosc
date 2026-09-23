package br.univille.sendhemosc.domain.dto;

import static org.assertj.core.api.Assertions.assertThat;

import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Quando a rodada automatica e devida. Janela das 8h as 18h, todo dia, salvo indicacao.
 */
@DisplayName("Agenda do disparo automatico")
class ConfiguracaoDisparoAutomaticoTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 9, 23);

    private ConfiguracaoDisparoAutomatico configuracao(final boolean ativo, final int intervaloDias,
                                                       final LocalDateTime ultimaRodada) {
        return new ConfiguracaoDisparoAutomatico(ativo, EnumSet.allOf(TipoSanguineo.class), 50, 8, 18,
                intervaloDias, ultimaRodada, null, "chefe@example.org", null);
    }

    private LocalDateTime hojeAs(final int hora) {
        return HOJE.atTime(hora, 15);
    }

    @Nested
    @DisplayName("rodada devida")
    class RodadaDevida {

        @Test
        @DisplayName("desligado nunca e devido, mesmo dentro da janela")
        void desligado() {
            assertThat(configuracao(false, 1, null).rodadaDevida(hojeAs(10))).isFalse();
        }

        @Test
        @DisplayName("ligado e sem rodada anterior, dentro da janela: devida")
        void primeiraRodada() {
            assertThat(configuracao(true, 1, null).rodadaDevida(hojeAs(10))).isTrue();
        }

        @Test
        @DisplayName("fora da janela nao e devida: ninguem recebe convite de madrugada")
        void foraDaJanela() {
            final ConfiguracaoDisparoAutomatico ligado = configuracao(true, 1, null);

            assertThat(ligado.rodadaDevida(hojeAs(3))).isFalse();
            assertThat(ligado.rodadaDevida(hojeAs(7))).isFalse();
        }

        @Test
        @DisplayName("a hora de fim nao entra na janela")
        void fimExclusivo() {
            final ConfiguracaoDisparoAutomatico ligado = configuracao(true, 1, null);

            assertThat(ligado.rodadaDevida(hojeAs(17))).isTrue();
            assertThat(ligado.rodadaDevida(hojeAs(18))).isFalse();
        }

        @Test
        @DisplayName("ja rodou hoje: nao roda de novo no mesmo dia")
        void jaRodouHoje() {
            assertThat(configuracao(true, 1, hojeAs(8)).rodadaDevida(hojeAs(14))).isFalse();
        }

        @Test
        @DisplayName("rodou ontem as 17h: hoje as 8h ja e devida, sem esperar 24 horas")
        void diaDeCalendario() {
            final LocalDateTime ontemTarde = HOJE.minusDays(1).atTime(17, 40);

            assertThat(configuracao(true, 1, ontemTarde).rodadaDevida(hojeAs(8))).isTrue();
        }

        @Test
        @DisplayName("uma vez por semana: tres dias depois ainda nao, sete dias depois sim")
        void semanal() {
            assertThat(configuracao(true, 7, HOJE.minusDays(3).atTime(9, 0)).rodadaDevida(hojeAs(10))).isFalse();
            assertThat(configuracao(true, 7, HOJE.minusDays(7).atTime(9, 0)).rodadaDevida(hojeAs(10))).isTrue();
        }

        @Test
        @DisplayName("servico parado por dias: roda uma vez ao voltar, e nao uma por dia perdido")
        void naoAcumulaAtraso() {
            final ConfiguracaoDisparoAutomatico parado = configuracao(true, 1, HOJE.minusDays(6).atTime(9, 0));

            assertThat(parado.rodadaDevida(hojeAs(10))).isTrue();
        }
    }

    @Nested
    @DisplayName("proxima rodada exibida na tela")
    class ProximaRodada {

        @Test
        @DisplayName("desligado nao tem proxima rodada")
        void desligado() {
            assertThat(configuracao(false, 1, null).proximaRodada(hojeAs(10))).isNull();
        }

        @Test
        @DisplayName("antes da janela: hoje, na abertura")
        void antesDaJanela() {
            assertThat(configuracao(true, 1, null).proximaRodada(hojeAs(6))).isEqualTo(HOJE.atTime(8, 0));
        }

        @Test
        @DisplayName("dentro da janela e devida: agora")
        void agora() {
            final LocalDateTime agora = hojeAs(10);

            assertThat(configuracao(true, 1, null).proximaRodada(agora)).isEqualTo(agora);
        }

        @Test
        @DisplayName("depois da janela: amanha, na abertura")
        void depoisDaJanela() {
            assertThat(configuracao(true, 1, null).proximaRodada(hojeAs(20))).isEqualTo(HOJE.plusDays(1).atTime(8, 0));
        }

        @Test
        @DisplayName("ja rodou hoje, a cada 3 dias: daqui a 3 dias, na abertura")
        void respeitaIntervalo() {
            assertThat(configuracao(true, 3, hojeAs(8)).proximaRodada(hojeAs(9)))
                    .isEqualTo(HOJE.plusDays(3).atTime(8, 0));
        }
    }
}
