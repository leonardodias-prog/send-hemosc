package br.univille.sendhemosc.usecase.estoque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.univille.sendhemosc.domain.dto.SituacaoEstoque;
import br.univille.sendhemosc.domain.enums.NivelEstoque;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IEstoqueRepositoryPort.RegistroEstoque;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Classificacao do nivel de estoque")
class ClassificarNivelEstoqueUseCaseTest {

    private static final int MINIMO_CRITICO = 5;
    private static final int MINIMO_BAIXO = 10;

    private final ClassificarNivelEstoqueUseCase useCase = new ClassificarNivelEstoqueUseCase();

    private SituacaoEstoque classificar(final int quantidade, final int capacidade) {
        return useCase.execute(new RegistroEstoque(TipoSanguineo.O_NEGATIVO, quantidade, capacidade,
                MINIMO_CRITICO, MINIMO_BAIXO));
    }

    @ParameterizedTest(name = "{0} bolsas, critico < 5 e baixo < 10 -> {1}")
    @CsvSource({
        "0,  CRITICO",
        "4,  CRITICO",
        "5,  ATENCAO",
        "9,  ATENCAO",
        "10, NORMAL",
        "25, NORMAL"
    })
    @DisplayName("os minimos em bolsas do tipo separam os tres niveis")
    void classificaPelosMinimosDoTipo(final int quantidade, final NivelEstoque nivelEsperado) {
        assertThat(classificar(quantidade, 40).nivel()).isEqualTo(nivelEsperado);
    }

    @Test
    @DisplayName("a capacidade alvo da o percentual, mas nao decide o nivel")
    void capacidadeNaoDecideONivel() {
        final SituacaoEstoque folgada = classificar(8, 10);
        final SituacaoEstoque apertada = classificar(8, 200);

        assertThat(folgada.percentualOcupacao()).isEqualTo(80);
        assertThat(apertada.percentualOcupacao()).isEqualTo(4);
        assertThat(folgada.nivel()).isEqualTo(apertada.nivel()).isEqualTo(NivelEstoque.ATENCAO);
    }

    @Test
    @DisplayName("apenas baixo e critico exigem convocacao, e critico e o mais grave")
    void apenasNiveisBaixosConvocam() {
        assertThat(classificar(2, 40).nivel().isExigeConvocacao()).isTrue();
        assertThat(classificar(7, 40).nivel().isExigeConvocacao()).isTrue();
        assertThat(classificar(30, 40).nivel().isExigeConvocacao()).isFalse();

        assertThat(NivelEstoque.CRITICO.maisGraveQue(NivelEstoque.ATENCAO)).isTrue();
        assertThat(NivelEstoque.ATENCAO.maisGraveQue(NivelEstoque.NORMAL)).isTrue();
        assertThat(NivelEstoque.NORMAL.maisGraveQue(NivelEstoque.ATENCAO)).isFalse();
        assertThat(NivelEstoque.ATENCAO.maisGraveQue(NivelEstoque.ATENCAO)).isFalse();
    }

    @Test
    @DisplayName("quantidade negativa e rejeitada")
    void rejeitaQuantidadeNegativa() {
        assertThatThrownBy(() -> classificar(-1, 100))
                .isInstanceOf(NegocioException.class)
                .hasMessage("estoque.quantidade.negativa");
    }

    @Test
    @DisplayName("capacidade alvo zerada e rejeitada, evitando divisao por zero")
    void rejeitaCapacidadeZero() {
        assertThatThrownBy(() -> classificar(10, 0))
                .isInstanceOf(NegocioException.class)
                .hasMessage("estoque.capacidade.invalida");
    }
}
