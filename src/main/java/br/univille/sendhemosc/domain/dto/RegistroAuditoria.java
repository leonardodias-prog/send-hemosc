package br.univille.sendhemosc.domain.dto;

import java.time.LocalDateTime;

/**
 * Uma acao registrada na auditoria, como aparece na tela.
 *
 * @param acao identificador curto da acao, em maiusculas
 * @param detalhe descricao do que foi feito
 * @param autor e-mail de quem fez, ou "sistema"
 * @param ocorridoEm instante da acao
 */
public record RegistroAuditoria(String acao, String detalhe, String autor, LocalDateTime ocorridoEm) {
}
