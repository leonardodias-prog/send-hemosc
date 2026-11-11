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
    CAPACIDADE_INVALIDA("EST-003", "estoque.capacidade.invalida", HttpStatus.BAD_REQUEST),
    MINIMOS_INVALIDOS("EST-004", "estoque.minimos.invalidos", HttpStatus.BAD_REQUEST),
    AVISO_HORA_INVALIDA("EST-005", "estoque.aviso.hora.invalida", HttpStatus.BAD_REQUEST),
    AVISO_SEM_DESTINATARIOS("EST-006", "estoque.aviso.destinatarios.vazio", HttpStatus.BAD_REQUEST);

    private final String codigo;
    private final String chaveMensagem;
    private final HttpStatus status;
}
