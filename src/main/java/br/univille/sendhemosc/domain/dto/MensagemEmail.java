package br.univille.sendhemosc.domain.dto;

/**
 * Mensagem pronta para envio, ja renderizada.
 *
 * @param destinatario endereco de e-mail do destinatario
 * @param assunto assunto da mensagem
 * @param corpoHtml corpo da mensagem em HTML
 */
public record MensagemEmail(String destinatario, String assunto, String corpoHtml) {
}
