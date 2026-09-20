package br.univille.sendhemosc.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Catalogo de erros do contexto de notificacao ao doador.
 */
@Getter
@RequiredArgsConstructor
public enum NotificacaoErrorsMessage implements ErrorsMessage {

    SEM_DESTINATARIOS("NOT-001", "notificacao.sem-destinatarios", HttpStatus.NOT_FOUND),
    FALHA_ENVIO("NOT-002", "notificacao.falha-envio", HttpStatus.SERVICE_UNAVAILABLE);

    private final String codigo;
    private final String chaveMensagem;
    private final HttpStatus status;
}
