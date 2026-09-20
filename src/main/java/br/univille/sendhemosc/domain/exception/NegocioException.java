package br.univille.sendhemosc.domain.exception;

import lombok.Getter;

/**
 * Excecao de regra de negocio. Carrega o catalogo de erro correspondente, de onde saem
 * o codigo, a chave da mensagem traduzida e o status HTTP.
 */
@Getter
public class NegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient ErrorsMessage erro;

    public NegocioException(final ErrorsMessage erro) {
        super(erro.getChaveMensagem());
        this.erro = erro;
    }

    public NegocioException(final ErrorsMessage erro, final Throwable causa) {
        super(erro.getChaveMensagem(), causa);
        this.erro = erro;
    }
}
