package br.univille.sendhemosc.usecase.usuario;

import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.exception.UsuarioErrorsMessage;
import java.nio.charset.StandardCharsets;

/**
 * Regra da senha nova, a mesma para quem troca a propria e para quem redefine pelo link.
 *
 * <p>O teto e contado em bytes, e nao em caracteres: e o limite do BCrypt, que nao guarda o que
 * passa dele. Letra acentuada ocupa dois bytes, entao nem sempre cabem 72 caracteres.</p>
 */
final class RegraDeSenha {

    private static final int TAMANHO_MINIMO = 8;
    private static final int TAMANHO_MAXIMO_EM_BYTES = 72;

    private RegraDeSenha() {
    }

    /**
     * Confere a senha nova e a repeticao dela.
     *
     * @param nova senha escolhida
     * @param confirmacao repeticao digitada
     */
    static void validar(final String nova, final String confirmacao) {
        if (nova == null || nova.length() < TAMANHO_MINIMO || excedeOLimite(nova)) {
            throw new NegocioException(UsuarioErrorsMessage.SENHA_FORA_DO_TAMANHO);
        }

        if (!nova.equals(confirmacao)) {
            throw new NegocioException(UsuarioErrorsMessage.SENHAS_DIFERENTES);
        }
    }

    /**
     * Indica se a senha passa do que o BCrypt aceita. Senha assim nunca e a de ninguem, e
     * confere-la contra um hash faria o codificador recusar a entrada com erro.
     *
     * @param senha senha digitada
     * @return true quando passa de 72 bytes
     */
    static boolean excedeOLimite(final String senha) {
        return senha.getBytes(StandardCharsets.UTF_8).length > TAMANHO_MAXIMO_EM_BYTES;
    }
}
