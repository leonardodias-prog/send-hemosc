package br.univille.sendhemosc.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Catalogo de erros da configuracao do disparo automatico.
 */
@Getter
@RequiredArgsConstructor
public enum DisparoErrorsMessage implements ErrorsMessage {

    LIMITE_INVALIDO("DIS-001", "disparo.limite.invalido", HttpStatus.BAD_REQUEST),
    JANELA_INVALIDA("DIS-002", "disparo.janela.invalida", HttpStatus.BAD_REQUEST),
    INTERVALO_INVALIDO("DIS-003", "disparo.intervalo.invalido", HttpStatus.BAD_REQUEST),
    SEM_TIPOS("DIS-004", "disparo.tipos.vazio", HttpStatus.BAD_REQUEST);

    private final String codigo;
    private final String chaveMensagem;
    private final HttpStatus status;
}
