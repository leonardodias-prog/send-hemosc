package br.univille.sendhemosc.domain.dto;

import java.time.LocalDate;

/**
 * Resultado da avaliacao dos limites de contato de um doador: se ainda cabe convoca-lo agora.
 *
 * @param liberado se a pessoa pode receber mais uma convocacao na data avaliada
 * @param tetoAtingido se quem retem e o teto de convocacoes sem resposta, e nao o intervalo
 * @param liberadoEm a partir de quando o intervalo deixa de reter; nulo quando a pessoa esta
 *                   liberada ou quando a retencao e pelo teto, que se desfaz com o prazo, com uma
 *                   doacao ou com a liberacao feita pelo administrador
 */
public record LimiteDeContato(boolean liberado, boolean tetoAtingido, LocalDate liberadoEm) {

    public static LimiteDeContato livre() {
        return new LimiteDeContato(true, false, null);
    }

    public static LimiteDeContato emIntervalo(final LocalDate liberadoEm) {
        return new LimiteDeContato(false, false, liberadoEm);
    }

    public static LimiteDeContato noTeto() {
        return new LimiteDeContato(false, true, null);
    }
}
