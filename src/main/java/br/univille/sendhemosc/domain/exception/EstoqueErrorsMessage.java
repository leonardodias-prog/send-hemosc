package br.univille.sendhemosc.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Catalogo de erros do contexto de estoque de hemocomponentes.
 */
@Getter
@RequiredArgsConstructor
public enum EstoqueErrorsMessage implements ErrorsMessage {

    NAO_ENCONTRADO("EST-001", "estoque.nao-encontrado", HttpStatus.NOT_FOUND),
    QUANTIDADE_NEGATIVA("EST-002", "estoque.quantidade.negativa", HttpStatus.BAD_REQUEST),
    CAPACIDADE_INVALIDA("EST-003", "estoque.capacidade.invalida", HttpStatus.BAD_REQUEST);

    private final String codigo;
    private final String chaveMensagem;
    private final HttpStatus status;
}
