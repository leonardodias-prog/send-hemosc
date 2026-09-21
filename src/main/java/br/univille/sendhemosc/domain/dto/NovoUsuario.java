package br.univille.sendhemosc.domain.dto;

import br.univille.sendhemosc.domain.enums.PerfilUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados do autocadastro de um funcionario.
 *
 * @param nome nome completo
 * @param email endereco de e-mail, usado para entrar no sistema
 * @param senha senha em texto, convertida em hash antes de qualquer persistencia
 * @param confirmacaoSenha repeticao da senha, conferida no caso de uso
 * @param perfil perfil pretendido
 */
public record NovoUsuario(
        @NotBlank(message = "{usuario.nome.obrigatorio}")
        @Size(max = 150)
        String nome,

        @NotBlank(message = "{usuario.email.obrigatorio}")
        @Email(message = "{usuario.email.invalido}")
        @Size(max = 180)
        String email,

        @NotBlank(message = "{usuario.senha.obrigatoria}")
        @Size(min = 8, max = 72, message = "{usuario.senha.tamanho}")
        String senha,

        @NotBlank(message = "{usuario.confirmacao.obrigatoria}")
        String confirmacaoSenha,

        @NotNull(message = "{usuario.perfil.obrigatorio}")
        PerfilUsuario perfil) {
}
