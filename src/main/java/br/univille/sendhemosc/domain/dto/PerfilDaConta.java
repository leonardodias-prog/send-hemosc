package br.univille.sendhemosc.domain.dto;

/**
 * O que a tela de perfil mostra da propria conta.
 *
 * @param usuario dados da conta, sem hash de senha nem token
 * @param temFoto true quando a pessoa enviou uma foto; senao a tela mostra a inicial do nome
 */
public record PerfilDaConta(UsuarioResumo usuario, boolean temFoto) {

    private static final String SEM_INICIAL = "?";

    /**
     * Primeira letra do nome, em maiuscula, para o circulo que aparece no lugar da foto. Lida por
     * ponto de codigo, e nao por posicao: nome que comeca por um caractere fora do plano basico
     * seria cortado ao meio.
     *
     * @return a inicial, ou "?" quando o nome esta vazio
     */
    public String inicial() {
        final String nome = usuario.nome() == null ? "" : usuario.nome().strip();

        if (nome.isEmpty()) {
            return SEM_INICIAL;
        }

        return new String(Character.toChars(Character.toUpperCase(nome.codePointAt(0))));
    }
}
