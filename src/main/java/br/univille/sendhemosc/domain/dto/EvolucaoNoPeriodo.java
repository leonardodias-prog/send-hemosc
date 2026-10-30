package br.univille.sendhemosc.domain.dto;

import java.util.List;

/**
 * Convocacoes enviadas e doacoes registradas ao longo de um periodo, em intervalos iguais.
 *
 * @param pontos um por intervalo, do mais antigo ao mais recente
 * @param intervalo "dia" ou "semana", conforme o tamanho do periodo
 */
public record EvolucaoNoPeriodo(List<Ponto> pontos, String intervalo) {

    /**
     * Maior valor da serie, que define a escala do grafico. Nunca zero, para a escala existir
     * mesmo num periodo sem movimento.
     *
     * @return maior quantidade entre convocacoes e doacoes
     */
    public long maximo() {
        return Math.max(1, pontos.stream()
                .mapToLong(ponto -> Math.max(ponto.convocacoes(), ponto.doacoes()))
                .max()
                .orElse(0));
    }

    /**
     * Um intervalo da serie.
     *
     * @param rotulo data de inicio do intervalo, no formato dd/MM
     * @param convocacoes convocacoes enviadas no intervalo
     * @param doacoes doacoes registradas no intervalo
     */
    public record Ponto(String rotulo, long convocacoes, long doacoes) {
    }
}
