package br.univille.sendhemosc.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Contrato comum dos catalogos de erro. Cada contexto de negocio expoe o seu proprio enum,
 * de modo que o codigo do erro identifique o contexto de origem.
 */
public interface ErrorsMessage {

    String getCodigo();

    String getChaveMensagem();

    HttpStatus getStatus();
}
