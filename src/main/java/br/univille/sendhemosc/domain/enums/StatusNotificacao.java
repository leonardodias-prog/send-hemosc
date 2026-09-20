package br.univille.sendhemosc.domain.enums;

/**
 * Ciclo de vida de uma convocacao enviada ao doador.
 */
public enum StatusNotificacao {

    PENDENTE,
    ENVIADA,
    FALHA,
    COMPARECEU,
    DESCADASTRADO;
}
