package br.univille.sendhemosc.domain.dto;

import static org.assertj.core.api.Assertions.assertThat;

import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Siglas aceitas pela busca, de acordo com o tipo sanguineo e o modo de compatibilidade.
 */
@DisplayName("Filtro de doadores")
class FiltroDoadorTest {

    @Test
    @DisplayName("sem tipo escolhido, aceita todas as siglas")
    void semTipo() {
        final FiltroDoador filtro = new FiltroDoador(null, null, false, false, false, false);

        assertThat(filtro.siglasAceitas()).hasSize(TipoSanguineo.values().length);
    }

    @Test
    @DisplayName("com tipo e sem compatibilidade, aceita so a sigla exata")
    void tipoExato() {
        final FiltroDoador filtro = new FiltroDoador(null, TipoSanguineo.A_POSITIVO, false, false, false, false);

        assertThat(filtro.siglasAceitas()).containsExactly("A+");
    }

    @Test
    @DisplayName("com tipo e compatibilidade, aceita quem pode doar para o tipo escolhido")
    void tipoPorCompatibilidade() {
        final FiltroDoador filtro = new FiltroDoador(null, TipoSanguineo.A_POSITIVO, false, false, true, false);

        assertThat(filtro.siglasAceitas()).containsExactlyInAnyOrderElementsOf(Set.of("A+", "A-", "O+", "O-"));
    }
}
