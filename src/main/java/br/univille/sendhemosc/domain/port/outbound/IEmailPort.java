package br.univille.sendhemosc.domain.port.outbound;

import br.univille.sendhemosc.domain.dto.MensagemEmail;

/**
 * Porta de saida para envio de e-mail. Existem duas implementacoes: uma que apenas registra
 * a mensagem em log, usada como padrao no perfil de desenvolvimento, e outra que envia por SMTP.
 */
public interface IEmailPort {

    /**
     * Envia a mensagem informada.
     *
     * @param mensagem mensagem ja renderizada
     */
    void enviar(MensagemEmail mensagem);
}
