package br.univille.sendhemosc.domain.enums;

/**
 * Perfis de acesso do sistema, do mais restrito ao mais amplo.
 * Quem dispara convocacao produz efeito fora do sistema: e-mail sai e nao volta. Por isso a
 * separacao entre quem alimenta os dados e quem decide acionar os doadores.
 */
public enum PerfilUsuario {

    /**
     * Funcionario comum. Cadastra doador, atualiza estoque e consulta a situacao.
     * Nao dispara convocacao. Cadastro proprio, liberado na hora.
     */
    OPERADOR("Operador", "Cadastra doadores, atualiza estoque e consulta"),

    /**
     * Enfermeiro chefe ou responsavel pela captacao. Alem do que o operador faz, dispara
     * convocacao e liga ou desliga o envio real. Cadastro proprio, mas depende de aprovacao.
     */
    RESPONSAVEL("Responsável", "Dispara convocações e controla o envio de e-mail"),

    /**
     * Administrador do sistema. Alem de tudo, aprova cadastros de responsavel e gerencia
     * usuarios. Existe desde a primeira subida e nao pode ser criado pela tela.
     */
    MASTER("Administrador", "Aprova responsáveis e gerencia usuários");

    private final String descricao;
    private final String atribuicoes;

    PerfilUsuario(final String descricao, final String atribuicoes) {
        this.descricao = descricao;
        this.atribuicoes = atribuicoes;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getAtribuicoes() {
        return atribuicoes;
    }

    /**
     * Indica se o perfil pode disparar convocacao e controlar o envio.
     *
     * @return true para responsavel e administrador
     */
    public boolean podeConvocar() {
        return this != OPERADOR;
    }

    /**
     * Indica se o cadastro neste perfil depende de aprovacao de um administrador.
     *
     * @return true para responsavel
     */
    public boolean exigeAprovacao() {
        return this == RESPONSAVEL;
    }

    /**
     * Indica se o perfil pode ser escolhido por quem se cadastra pela tela.
     *
     * @return false para administrador, que nao se cria pela tela
     */
    public boolean disponivelNoAutocadastro() {
        return this != MASTER;
    }
}
