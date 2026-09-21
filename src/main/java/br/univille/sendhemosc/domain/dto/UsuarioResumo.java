package br.univille.sendhemosc.domain.dto;

import br.univille.sendhemosc.domain.enums.PerfilUsuario;
import br.univille.sendhemosc.domain.enums.SituacaoUsuario;
import java.time.LocalDateTime;

/**
 * Projecao de um usuario para exibicao. Nao carrega hash de senha nem token de aprovacao.
 *
 * @param id identificador
 * @param nome nome completo
 * @param email endereco de e-mail
 * @param perfil perfil de acesso
 * @param situacao situacao da conta
 * @param criadoEm quando o cadastro foi feito
 * @param ultimoAcessoEm ultimo acesso registrado, nulo para quem nunca entrou
 */
public record UsuarioResumo(
        Long id,
        String nome,
        String email,
        PerfilUsuario perfil,
        SituacaoUsuario situacao,
        LocalDateTime criadoEm,
        LocalDateTime ultimoAcessoEm) {
}
