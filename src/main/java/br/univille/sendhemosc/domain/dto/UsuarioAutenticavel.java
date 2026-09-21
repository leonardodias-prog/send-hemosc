package br.univille.sendhemosc.domain.dto;

import br.univille.sendhemosc.domain.enums.PerfilUsuario;
import br.univille.sendhemosc.domain.enums.SituacaoUsuario;

/**
 * Dados minimos para autenticar. Sai do dominio apenas para a camada de seguranca.
 *
 * @param id identificador
 * @param nome nome completo
 * @param email endereco usado para entrar
 * @param senhaHash hash BCrypt da senha
 * @param perfil perfil de acesso
 * @param situacao situacao da conta
 */
public record UsuarioAutenticavel(
        Long id,
        String nome,
        String email,
        String senhaHash,
        PerfilUsuario perfil,
        SituacaoUsuario situacao) {
}
