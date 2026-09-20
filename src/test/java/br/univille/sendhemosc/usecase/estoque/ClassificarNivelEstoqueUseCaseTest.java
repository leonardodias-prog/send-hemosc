package br.univille.sendhemosc.usecase.estoque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.univille.sendhemosc.config.SendHemoscProperties;
import br.univille.sendhemosc.domain.dto.SituacaoEstoque;
import br.univille.sendhemosc.domain.enums.NivelEstoque;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.exception.NegocioException;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Classificacao do nivel de estoque")
class ClassificarNivelEstoqueUseCaseTest {

    private ClassificarNivelEstoqueUseCase useCase;

    @BeforeEach
    void preparar() {
        final var aptidao = new SendHemoscProperties.Aptidao(60, 90, 4, 3, 16, 69, BigDecimal.valueOf(50));
        final var estoque = new SendHemoscProperties.Estoque(30, 60);
        final var notificacao = new SendHemoscProperties.Notificacao(30, "teste@example.org", "Teste", "http://localhost:8080");

        useCase = new ClassificarNivelEstoqueUseCase(new SendHemoscProperties(aptidao, estoque, notificacao));
    }

    @ParameterizedTest(name = "{0} de {1} bolsas -> {2}% -> {3}")
    @CsvSource({
        "0,   100, 0,   CRITICO",
        "29,  100, 29,  CRITICO",
        "30,  100, 30,  ATENCAO",
        "59,  100, 59,  ATENCAO",
        "60,  100, 60,  NORMAL",
        "100, 100, 100, NORMAL",
        "150, 100, 150, NORMAL"
    })
    @DisplayName("os limiares percentuais separam os tres niveis")
    void classificaPorPercentual(final int quantidade, final int capacidade,
                                 final int percentualEsperado, final NivelEstoque nivelEsperado) {
        final SituacaoEstoque resultado = useCase.execute(TipoSanguineo.O_NEGATIVO, quantidade, capacidade);

        assertThat(resultado.percentualOcupacao()).isEqualTo(percentualEsperado);
        assertThat(resultado.nivel()).isEqualTo(nivelEsperado);
    }

    @Test
    @DisplayName("apenas ATENCAO e CRITICO exigem convocacao")
    void apenasNiveisBaixosConvocam() {
        assertThat(useCase.execute(TipoSanguineo.A_POSITIVO, 10, 100).nivel().isExigeConvocacao()).isTrue();
        assertThat(useCase.execute(TipoSanguineo.A_POSITIVO, 45, 100).nivel().isExigeConvocacao()).isTrue();
        assertThat(useCase.execute(TipoSanguineo.A_POSITIVO, 90, 100).nivel().isExigeConvocacao()).isFalse();
    }

    @Test
    @DisplayName("quantidade negativa e rejeitada")
    void rejeitaQuantidadeNegativa() {
        assertThatThrownBy(() -> useCase.execute(TipoSanguineo.B_POSITIVO, -1, 100))
                .isInstanceOf(NegocioException.class)
                .hasMessage("estoque.quantidade.negativa");
    }

    @Test
    @DisplayName("capacidade alvo zerada e rejeitada, evitando divisao por zero")
    void rejeitaCapacidadeZero() {
        assertThatThrownBy(() -> useCase.execute(TipoSanguineo.B_POSITIVO, 10, 0))
                .isInstanceOf(NegocioException.class)
                .hasMessage("estoque.capacidade.invalida");
    }
}
