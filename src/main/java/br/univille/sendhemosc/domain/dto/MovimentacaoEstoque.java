package br.univille.sendhemosc.domain.dto;

import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import java.time.LocalDateTime;

/**
 * Uma alteracao registrada no estoque de um tipo sanguineo.
 *
 * @param tipoSanguineo tipo alterado
 * @param quantidadeAnterior bolsas antes da alteracao
 * @param quantidadeNova bolsas depois da alteracao
 * @param capacidadeAnterior capacidade alvo antes da alteracao
 * @param capacidadeNova capacidade alvo depois da alteracao
 * @param responsavel e-mail de quem alterou, ou "sistema"
 * @param ocorridoEm instante da alteracao
 */
public record MovimentacaoEstoque(
        TipoSanguineo tipoSanguineo,
        int quantidadeAnterior,
        int quantidadeNova,
        int capacidadeAnterior,
        int capacidadeNova,
        String responsavel,
        LocalDateTime ocorridoEm) {

    /**
     * Diferenca de bolsas: positiva numa entrada, negativa numa saida.
     *
     * @return quantidade nova menos a anterior
     */
    public int variacaoBolsas() {
        return quantidadeNova - quantidadeAnterior;
    }

    /**
     * Informa se a alteracao mexeu na capacidade alvo.
     *
     * @return true quando a capacidade mudou
     */
    public boolean capacidadeAlterada() {
        return capacidadeNova != capacidadeAnterior;
    }
}
