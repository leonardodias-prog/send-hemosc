package br.univille.sendhemosc.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tipos sanguineos do sistema ABO/Rh e a relacao de compatibilidade para doacao.
 * As relacoes reproduzem a tabela de compatibilidade transfusional de concentrado de hemacias
 * e devem ser validadas formalmente pela equipe de hemoterapia antes de qualquer uso real.
 */
public enum TipoSanguineo {

    A_POSITIVO("A+"),
    A_NEGATIVO("A-"),
    B_POSITIVO("B+"),
    B_NEGATIVO("B-"),
    AB_POSITIVO("AB+"),
    AB_NEGATIVO("AB-"),
    O_POSITIVO("O+"),
    O_NEGATIVO("O-");

    private final String sigla;

    TipoSanguineo(final String sigla) {
        this.sigla = sigla;
    }

    @JsonValue
    public String getSigla() {
        return sigla;
    }

    /**
     * Converte a sigla textual (ex.: "A+") para o enum correspondente.
     *
     * @param sigla sigla do tipo sanguineo
     * @return o tipo sanguineo correspondente
     */
    public static TipoSanguineo doSigla(final String sigla) {
        return Arrays.stream(values())
                .filter(tipo -> tipo.sigla.equalsIgnoreCase(sigla))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tipo sanguineo desconhecido: " + sigla));
    }

    /**
     * Retorna os tipos sanguineos capazes de doar para este tipo. E o conjunto usado na convocacao
     * segmentada: quando este tipo esta em falta, sao estas as pessoas que podem ajudar.
     *
     * @return conjunto de tipos sanguineos doadores compativeis
     */
    public Set<TipoSanguineo> doadoresCompativeis() {
        return switch (this) {
            case A_POSITIVO -> Set.of(A_POSITIVO, A_NEGATIVO, O_POSITIVO, O_NEGATIVO);
            case A_NEGATIVO -> Set.of(A_NEGATIVO, O_NEGATIVO);
            case B_POSITIVO -> Set.of(B_POSITIVO, B_NEGATIVO, O_POSITIVO, O_NEGATIVO);
            case B_NEGATIVO -> Set.of(B_NEGATIVO, O_NEGATIVO);
            case AB_POSITIVO -> Set.of(values());
            case AB_NEGATIVO -> Set.of(A_NEGATIVO, B_NEGATIVO, AB_NEGATIVO, O_NEGATIVO);
            case O_POSITIVO -> Set.of(O_POSITIVO, O_NEGATIVO);
            case O_NEGATIVO -> Set.of(O_NEGATIVO);
        };
    }

    /**
     * Siglas dos doadores compativeis, para uso em consultas de persistencia.
     *
     * @return conjunto de siglas
     */
    public Set<String> siglasDoadoresCompativeis() {
        return doadoresCompativeis().stream()
                .map(TipoSanguineo::getSigla)
                .collect(Collectors.toSet());
    }
}
