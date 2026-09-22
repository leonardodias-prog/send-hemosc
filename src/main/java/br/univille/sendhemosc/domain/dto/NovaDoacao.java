package br.univille.sendhemosc.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Registro de uma doacao efetivada.
 *
 * @param doadorId quem doou
 * @param dataDoacao quando doou; nao pode ser futura
 * @param localColeta onde a coleta aconteceu, opcional
 * @param observacao anotacao livre da equipe, opcional
 */
public record NovaDoacao(
        @NotNull(message = "{doacao.doador.obrigatorio}")
        Long doadorId,

        @NotNull(message = "{doacao.data.obrigatoria}")
        @PastOrPresent(message = "{doacao.data.futura}")
        LocalDate dataDoacao,

        @Size(max = 150)
        String localColeta,

        @Size(max = 500)
        String observacao) {
}
