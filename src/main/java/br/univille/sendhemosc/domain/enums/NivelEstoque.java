package br.univille.sendhemosc.domain.enums;

/**
 * Classificacao do estoque de um tipo sanguineo, conforme definido na Etapa 3 do projeto.
 * NORMAL nao dispara convocacao; ATENCAO e CRITICO disparam.
 */
public enum NivelEstoque {

    CRITICO("Critico", true),
    ATENCAO("Atencao", true),
    NORMAL("Normal", false);

    private final String descricao;
    private final boolean exigeConvocacao;

    NivelEstoque(final String descricao, final boolean exigeConvocacao) {
        this.descricao = descricao;
        this.exigeConvocacao = exigeConvocacao;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isExigeConvocacao() {
        return exigeConvocacao;
    }
}
