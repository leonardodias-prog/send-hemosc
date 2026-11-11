package br.univille.sendhemosc.domain.dto;

import br.univille.sendhemosc.domain.enums.TipoSanguineo;

/**
 * Os dois minimos de um tipo sanguineo, em bolsas.
 *
 * @param tipoSanguineo tipo a que se referem
 * @param minimoCritico abaixo desta quantidade o nivel e critico
 * @param minimoBaixo abaixo desta quantidade o nivel e baixo
 */
public record MinimosDoTipo(TipoSanguineo tipoSanguineo, int minimoCritico, int minimoBaixo) {
}
