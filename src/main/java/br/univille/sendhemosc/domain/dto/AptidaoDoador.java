package br.univille.sendhemosc.domain.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Resultado da avaliacao de aptidao de um doador em uma data de referencia.
 *
 * @param apto se o doador pode doar na data de referencia
 * @param proximaDataApta data estimada em que o doador volta a poder doar
 * @param motivosInaptidao lista de motivos, vazia quando o doador esta apto
 */
public record AptidaoDoador(boolean apto, LocalDate proximaDataApta, List<String> motivosInaptidao) {

    public static AptidaoDoador apto(final LocalDate desde) {
        return new AptidaoDoador(true, desde, List.of());
    }

    public static AptidaoDoador inapto(final LocalDate proximaDataApta, final List<String> motivos) {
        return new AptidaoDoador(false, proximaDataApta, motivos);
    }
}
