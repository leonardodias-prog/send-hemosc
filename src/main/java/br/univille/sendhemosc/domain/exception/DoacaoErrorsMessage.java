package br.univille.sendhemosc.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Catalogo de erros do registro de doacoes.
 */
@Getter
@RequiredArgsConstructor
public enum DoacaoErrorsMessage implements ErrorsMessage {

    DOADOR_NAO_ENCONTRADO("DOC-001", "doacao.doador.nao-encontrado", HttpStatus.NOT_FOUND),
    DATA_FUTURA("DOC-002", "doacao.data.futura", HttpStatus.BAD_REQUEST),
    JA_REGISTRADA_NO_DIA("DOC-003", "doacao.ja-registrada-no-dia", HttpStatus.CONFLICT);

    private final String codigo;
    private final String chaveMensagem;
    private final HttpStatus status;
}
