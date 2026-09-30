package br.univille.sendhemosc.domain.util;

import java.util.regex.Pattern;

/**
 * Esconde a maior parte de um e-mail antes de ele ir para o log.
 *
 * <p>O log de uma hospedagem e lido por quem opera a plataforma e costuma ser guardado por
 * mais tempo que o necessario. Endereco de doador e dado pessoal, e para investigar um
 * problema basta reconhecer o dominio e a primeira letra. O banco continua com o endereco
 * inteiro, onde o acesso e controlado.</p>
 */
public final class MascaraDeEmail {

    private static final Pattern ENDERECO = Pattern.compile("[\\w.%+-]+@[\\w-]+(?:\\.[\\w-]+)+");

    private MascaraDeEmail() {
    }

    /**
     * Mascara um unico endereco: "maria.silva@exemplo.com" vira "m***@exemplo.com".
     *
     * @param email endereco a esconder, possivelmente nulo
     * @return o endereco mascarado, ou o proprio valor quando nao tem o formato de e-mail
     */
    public static String mascarar(final String email) {
        if (email == null) {
            return null;
        }

        final int arroba = email.indexOf('@');

        if (arroba < 1) {
            return email;
        }

        return email.charAt(0) + "***" + email.substring(arroba);
    }

    /**
     * Mascara todo endereco que aparecer dentro de um texto livre, como a mensagem de uma
     * excecao ou o detalhe de um registro de auditoria.
     *
     * @param texto texto que pode conter enderecos, possivelmente nulo
     * @return o texto com cada endereco mascarado
     */
    public static String mascararEmTexto(final String texto) {
        if (texto == null) {
            return null;
        }

        return ENDERECO.matcher(texto).replaceAll(resultado -> mascarar(resultado.group()));
    }
}
