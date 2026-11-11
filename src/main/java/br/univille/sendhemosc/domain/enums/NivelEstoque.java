package br.univille.sendhemosc.domain.enums;

/**
 * Classificacao do estoque de um tipo sanguineo, conforme definido na Etapa 3 do projeto.
 * NORMAL nao dispara convocacao; ATENCAO e CRITICO disparam.
 *
 * <p>ATENCAO aparece para a equipe como "Baixo". O identificador ficou porque ja esta gravado
 * nas convocacoes registradas.</p>
 */
public enum NivelEstoque {

    // A ordem importa: vai do mais grave para o mais folgado, e maisGraveQue depende dela.
    CRITICO("Crítico", true),
    ATENCAO("Baixo", true),
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

    /**
     * Compara a gravidade de dois niveis.
     *
     * @param outro nivel de referencia
     * @return true quando este nivel e mais grave que o de referencia
     */
    public boolean maisGraveQue(final NivelEstoque outro) {
        return ordinal() < outro.ordinal();
    }
}
