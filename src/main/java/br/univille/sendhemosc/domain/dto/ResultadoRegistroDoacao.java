package br.univille.sendhemosc.domain.dto;

import java.time.LocalDate;

/**
 * Desfecho do registro de uma doacao.
 *
 * @param doacaoId identificador da doacao criada
 * @param nomeDoador nome de quem doou, para exibir na confirmacao
 * @param convocacoesFechadas quantas convocacoes pendentes passaram a constar como atendidas
 * @param proximaDataApta data estimada em que a pessoa volta a poder doar
 * @param estavaApto se a pessoa constava como apta no momento do registro
 */
public record ResultadoRegistroDoacao(
        Long doacaoId,
        String nomeDoador,
        int convocacoesFechadas,
        LocalDate proximaDataApta,
        boolean estavaApto) {
}
