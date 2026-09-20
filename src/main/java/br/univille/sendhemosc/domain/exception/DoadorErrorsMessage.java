package br.univille.sendhemosc.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Catalogo de erros do contexto de doador. A chave aponta para messages_pt_BR.properties.
 */
@Getter
@RequiredArgsConstructor
public enum DoadorErrorsMessage implements ErrorsMessage {

    NAO_ENCONTRADO("DOA-001", "doador.nao-encontrado", HttpStatus.NOT_FOUND),
    EMAIL_DUPLICADO("DOA-002", "doador.email.duplicado", HttpStatus.CONFLICT),
    INAPTO_IDADE("DOA-003", "doador.inapto.idade", HttpStatus.UNPROCESSABLE_ENTITY),
    INAPTO_PESO("DOA-004", "doador.inapto.peso", HttpStatus.UNPROCESSABLE_ENTITY),
    INAPTO_INTERVALO("DOA-005", "doador.inapto.intervalo", HttpStatus.UNPROCESSABLE_ENTITY),
    INAPTO_LIMITE_ANUAL("DOA-006", "doador.inapto.limite-anual", HttpStatus.UNPROCESSABLE_ENTITY);

    private final String codigo;
    private final String chaveMensagem;
    private final HttpStatus status;
}
