package br.univille.sendhemosc.domain.enums;

/**
 * Ciclo de vida da conta de um usuario.
 */
public enum SituacaoUsuario {

    /** Aguardando aprovacao de um administrador. Nao consegue entrar. */
    PENDENTE("Aguardando aprovação"),

    /** Conta liberada. */
    ATIVO("Ativo"),

    /** Cadastro recusado por um administrador. Nao consegue entrar. */
    RECUSADO("Recusado"),

    /** Conta desativada apos ter sido ativa. Nao consegue entrar. */
    INATIVO("Inativo");

    private final String descricao;

    SituacaoUsuario(final String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Indica se a conta permite autenticacao.
     *
     * @return true somente para conta ativa
     */
    public boolean permiteAcesso() {
        return this == ATIVO;
    }
}
