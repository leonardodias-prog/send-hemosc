package br.univille.sendhemosc;

import static org.assertj.core.api.Assertions.assertThat;

import br.univille.sendhemosc.adapter.outbound.email.LogEmailAdapter;
import br.univille.sendhemosc.domain.dto.SituacaoEstoque;
import br.univille.sendhemosc.domain.port.outbound.IEmailPort;
import br.univille.sendhemosc.usecase.estoque.ListarSituacaoEstoqueUseCase;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("Contexto da aplicacao")
class SendHemoscApplicationTest {

    @Autowired
    private ListarSituacaoEstoqueUseCase listarSituacaoEstoque;

    @Autowired
    private IEmailPort emailPort;

    @Test
    @DisplayName("as migrations criam o estoque dos oito tipos sanguineos")
    void migrationsCarregamEstoqueInicial() {
        final List<SituacaoEstoque> situacoes = listarSituacaoEstoque.execute();

        assertThat(situacoes).hasSize(8);
        assertThat(situacoes).extracting(situacao -> situacao.tipoSanguineo().getSigla())
                .containsExactlyInAnyOrder("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
    }

    @Test
    @DisplayName("a listagem vem ordenada do mais critico para o mais folgado")
    void listagemOrdenadaPorCriticidade() {
        final List<SituacaoEstoque> situacoes = listarSituacaoEstoque.execute();

        assertThat(situacoes).isSortedAccordingTo(
                (primeiro, segundo) -> Integer.compare(primeiro.percentualOcupacao(), segundo.percentualOcupacao()));
    }

    @Test
    @DisplayName("por padrao nenhum e-mail sai de verdade: o adapter ativo e o de log")
    void adapterSeguroPorPadrao() {
        assertThat(emailPort).isInstanceOf(LogEmailAdapter.class);
    }
}
