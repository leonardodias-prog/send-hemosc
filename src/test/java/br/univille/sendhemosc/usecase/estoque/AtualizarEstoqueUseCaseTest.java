package br.univille.sendhemosc.usecase.estoque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.univille.sendhemosc.domain.dto.MovimentacaoEstoque;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.exception.EstoqueErrorsMessage;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IEstoqueRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.IEstoqueRepositoryPort.RegistroEstoque;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Cobre a edicao da quantidade e da capacidade alvo, que antes so aceitava a quantidade.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Atualizacao do estoque")
class AtualizarEstoqueUseCaseTest {

    private static final TipoSanguineo TIPO = TipoSanguineo.O_NEGATIVO;

    @Mock
    private IEstoqueRepositoryPort estoqueRepository;

    private AtualizarEstoqueUseCase useCase;

    @BeforeEach
    void preparar() {
        useCase = new AtualizarEstoqueUseCase(estoqueRepository);
    }

    private void estoqueAtual(final int quantidade, final int capacidade) {
        when(estoqueRepository.buscarPorTipo(TIPO)).thenReturn(Optional.of(new RegistroEstoque(TIPO, quantidade, capacidade)));
    }

    @Test
    @DisplayName("grava a nova quantidade e a nova capacidade alvo")
    void atualizaQuantidadeECapacidade() {
        estoqueAtual(11, 60);
        final var movimentacao = new MovimentacaoEstoque(TIPO, 11, 15, 60, 80, "operador@example.org", LocalDateTime.now());
        when(estoqueRepository.atualizar(TIPO, 15, 80)).thenReturn(Optional.of(movimentacao));

        assertThat(useCase.execute(TIPO, 15, 80)).contains(movimentacao);
        assertThat(movimentacao.variacaoBolsas()).isEqualTo(4);
        assertThat(movimentacao.capacidadeAlterada()).isTrue();
    }

    @Test
    @DisplayName("sem capacidade informada, mantem a atual")
    void semCapacidadeMantemAAtual() {
        estoqueAtual(11, 60);
        when(estoqueRepository.atualizar(TIPO, 20, 60)).thenReturn(Optional.empty());

        useCase.execute(TIPO, 20, null);

        verify(estoqueRepository).atualizar(TIPO, 20, 60);
    }

    @Test
    @DisplayName("recusa quantidade negativa sem gravar nada")
    void recusaQuantidadeNegativa() {
        assertThatThrownBy(() -> useCase.execute(TIPO, -1, 60))
                .isInstanceOfSatisfying(NegocioException.class,
                        excecao -> assertThat(excecao.getErro()).isEqualTo(EstoqueErrorsMessage.QUANTIDADE_NEGATIVA));

        verify(estoqueRepository, never()).atualizar(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("recusa capacidade alvo zero: a classificacao divide por ela")
    void recusaCapacidadeZero() {
        assertThatThrownBy(() -> useCase.execute(TIPO, 10, 0))
                .isInstanceOfSatisfying(NegocioException.class,
                        excecao -> assertThat(excecao.getErro()).isEqualTo(EstoqueErrorsMessage.CAPACIDADE_INVALIDA));

        verify(estoqueRepository, never()).atualizar(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("recusa tipo sem registro de estoque")
    void recusaTipoSemRegistro() {
        when(estoqueRepository.buscarPorTipo(TIPO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(TIPO, 10, 60))
                .isInstanceOfSatisfying(NegocioException.class,
                        excecao -> assertThat(excecao.getErro()).isEqualTo(EstoqueErrorsMessage.NAO_ENCONTRADO));
    }
}
