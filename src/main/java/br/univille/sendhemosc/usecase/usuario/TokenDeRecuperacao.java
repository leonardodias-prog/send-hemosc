package br.univille.sendhemosc.usecase.usuario;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Token do link de recuperacao de senha.
 *
 * <p>O link leva o token; o banco guarda so o SHA-256 dele. Diferente da senha, o token ja e
 * aleatorio e longo, entao um hash rapido basta: nao ha dicionario de tokens para testar.</p>
 */
final class TokenDeRecuperacao {

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final int BYTES = 32;

    private TokenDeRecuperacao() {
    }

    static String gerar() {
        final byte[] bytes = new byte[BYTES];
        ALEATORIO.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(final String token) {
        try {
            final byte[] resumo = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(resumo);
        } catch (final NoSuchAlgorithmException excecao) {
            // Toda JVM traz SHA-256; faltar e defeito do ambiente, nao do pedido.
            throw new IllegalStateException("SHA-256 indisponivel", excecao);
        }
    }
}
