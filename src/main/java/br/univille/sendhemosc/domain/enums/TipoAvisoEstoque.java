package br.univille.sendhemosc.domain.enums;

/**
 * Motivo de um aviso interno de estoque.
 */
public enum TipoAvisoEstoque {

    RELATORIO_DIARIO("Relatório diário"),
    ALERTA("Alerta"),
    TESTE("Teste");

    private final String descricao;

    TipoAvisoEstoque(final String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
