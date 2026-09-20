package br.univille.sendhemosc.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Compatibilidade entre tipos sanguineos")
class TipoSanguineoTest {

    @Test
    @DisplayName("O negativo e doador universal: doa para todos os oito tipos")
    void doadorUniversal() {
        final long tiposQueRecebemDeONegativo = java.util.Arrays.stream(TipoSanguineo.values())
                .filter(tipo -> tipo.doadoresCompativeis().contains(TipoSanguineo.O_NEGATIVO))
                .count();

        assertThat(tiposQueRecebemDeONegativo).isEqualTo(TipoSanguineo.values().length);
    }

    @Test
    @DisplayName("AB positivo e receptor universal: aceita todos os tipos")
    void receptorUniversal() {
        assertThat(TipoSanguineo.AB_POSITIVO.doadoresCompativeis())
                .containsExactlyInAnyOrder(TipoSanguineo.values());
    }

    @Test
    @DisplayName("O negativo so pode receber de O negativo")
    void oNegativoRecebeApenasDeSiMesmo() {
        assertThat(TipoSanguineo.O_NEGATIVO.doadoresCompativeis())
                .containsExactly(TipoSanguineo.O_NEGATIVO);
    }

    @Test
    @DisplayName("A positivo aceita A+, A-, O+ e O-")
    void aPositivoAceitaQuatroTipos() {
        assertThat(TipoSanguineo.A_POSITIVO.doadoresCompativeis())
                .containsExactlyInAnyOrder(
                        TipoSanguineo.A_POSITIVO, TipoSanguineo.A_NEGATIVO,
                        TipoSanguineo.O_POSITIVO, TipoSanguineo.O_NEGATIVO);
    }

    @Test
    @DisplayName("um tipo Rh negativo nunca aceita doador Rh positivo")
    void rhNegativoNaoAceitaPositivo() {
        final var negativos = java.util.List.of(
                TipoSanguineo.A_NEGATIVO, TipoSanguineo.B_NEGATIVO,
                TipoSanguineo.AB_NEGATIVO, TipoSanguineo.O_NEGATIVO);

        for (final TipoSanguineo receptor : negativos) {
            assertThat(receptor.doadoresCompativeis())
                    .as("receptor %s", receptor.getSigla())
                    .allMatch(doador -> doador.getSigla().endsWith("-"));
        }
    }

    @Test
    @DisplayName("conversao a partir da sigla e tolerante a caixa")
    void conversaoDeSigla() {
        assertThat(TipoSanguineo.doSigla("AB+")).isEqualTo(TipoSanguineo.AB_POSITIVO);
        assertThat(TipoSanguineo.doSigla("o-")).isEqualTo(TipoSanguineo.O_NEGATIVO);
    }

    @Test
    @DisplayName("sigla desconhecida e rejeitada")
    void siglaInvalida() {
        assertThatThrownBy(() -> TipoSanguineo.doSigla("C+"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("as siglas de compatibilidade servem direto na consulta de persistencia")
    void siglasParaConsulta() {
        assertThat(TipoSanguineo.B_NEGATIVO.siglasDoadoresCompativeis())
                .containsExactlyInAnyOrder("B-", "O-");
    }
}
