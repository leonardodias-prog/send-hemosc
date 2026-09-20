package br.univille.sendhemosc.adapter.inbound.http;

import br.univille.sendhemosc.domain.exception.ErrorsMessage;
import br.univille.sendhemosc.domain.exception.NegocioException;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduz excecoes de negocio em respostas HTTP com codigo de erro e mensagem localizada.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<RespostaErro> tratarNegocio(final NegocioException excecao) {
        final ErrorsMessage erro = excecao.getErro();
        final String mensagem = messageSource.getMessage(
                erro.getChaveMensagem(), null, erro.getChaveMensagem(), Locale.of("pt", "BR"));

        log.warn("[m=tratarNegocio] {} - {}", erro.getCodigo(), mensagem);
        return ResponseEntity.status(erro.getStatus())
                .body(new RespostaErro(erro.getCodigo(), mensagem, LocalDateTime.now()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RespostaErro> tratarArgumentoInvalido(final IllegalArgumentException excecao) {
        log.warn("[m=tratarArgumentoInvalido] {}", excecao.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new RespostaErro("REQ-400", excecao.getMessage(), LocalDateTime.now()));
    }

    /**
     * Corpo padrao de erro da API.
     *
     * @param codigo codigo do catalogo de erro
     * @param mensagem mensagem traduzida
     * @param momento instante da ocorrencia
     */
    public record RespostaErro(String codigo, String mensagem, LocalDateTime momento) {
    }
}
