package br.univille.sendhemosc.domain.dto;

import br.univille.sendhemosc.domain.enums.PerfilUsuario;
import java.time.LocalDateTime;

/**
 * Cadastro de responsavel aguardando a decisao de um administrador.
 *
 * @param id identificador da conta
 * @param nome nome de quem se cadastrou
 * @param email e-mail de quem se cadastrou
 * @param perfil perfil solicitado
 * @param tokenAprovacao token dos links de decisao enviados por e-mail
 * @param criadoEm quando o cadastro foi feito; a validade do link conta a partir daqui
 */
public record CadastroPendente(Long id, String nome, String email, PerfilUsuario perfil,
                               String tokenAprovacao, LocalDateTime criadoEm) {

    /**
     * Indica se o link de decisao ja venceu.
     *
     * @param agora momento da avaliacao
     * @param validadeDias por quantos dias o link vale, contados do cadastro
     * @return true quando o prazo ja passou
     */
    public boolean linkVencido(final LocalDateTime agora, final int validadeDias) {
        return !criadoEm.plusDays(validadeDias).isAfter(agora);
    }
}
