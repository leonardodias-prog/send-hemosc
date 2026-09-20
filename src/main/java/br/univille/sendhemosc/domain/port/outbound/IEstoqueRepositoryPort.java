package br.univille.sendhemosc.domain.port.outbound;

import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import java.util.List;
import java.util.Optional;

/**
 * Porta de saida para o estoque de hemocomponentes.
 */
public interface IEstoqueRepositoryPort {

    /**
     * Retorna a quantidade atual e a capacidade alvo de todos os tipos sanguineos.
     *
     * @return lista de pares tipo/quantidade/capacidade
     */
    List<RegistroEstoque> listarTodos();

    /**
     * Busca o registro de estoque de um tipo especifico.
     *
     * @param tipoSanguineo tipo desejado
     * @return registro de estoque, se existir
     */
    Optional<RegistroEstoque> buscarPorTipo(TipoSanguineo tipoSanguineo);

    /**
     * Atualiza a quantidade de bolsas em estoque.
     *
     * @param tipoSanguineo tipo a atualizar
     * @param quantidadeBolsas nova quantidade
     */
    void atualizarQuantidade(TipoSanguineo tipoSanguineo, int quantidadeBolsas);

    /**
     * Registro bruto de estoque, sem a classificacao de nivel aplicada.
     *
     * @param tipoSanguineo tipo sanguineo
     * @param quantidadeBolsas quantidade atual
     * @param capacidadeAlvo capacidade considerada ideal
     */
    record RegistroEstoque(TipoSanguineo tipoSanguineo, int quantidadeBolsas, int capacidadeAlvo) {
    }
}
