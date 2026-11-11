package br.univille.sendhemosc.domain.dto;

import br.univille.sendhemosc.domain.enums.TipoAvisoEstoque;
import java.time.LocalDateTime;

/**
 * Um aviso interno de estoque que saiu.
 *
 * @param tipo motivo do aviso
 * @param assunto assunto do e-mail
 * @param destinatarios para quantos enderecos foi
 * @param falhas quantos desses envios falharam
 * @param enviadoEm quando saiu
 */
public record EnvioAvisoEstoque(
        TipoAvisoEstoque tipo,
        String assunto,
        int destinatarios,
        int falhas,
        LocalDateTime enviadoEm) {
}
