package br.univille.sendhemosc.domain.port.outbound;

import br.univille.sendhemosc.domain.dto.MovimentacaoEstoque;
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
     * Atualiza a quantidade de bolsas e a capacidade alvo, registrando a movimentacao com o
     * antes e o depois. Quando nada muda, nada e gravado.
     *
     * @param tipoSanguineo tipo a atualizar
     * @param quantidadeBolsas nova quantidade
     * @param capacidadeAlvo nova capacidade alvo
     * @return movimentacao registrada, ou vazio quando os valores ja eram esses
     */
    Optional<MovimentacaoEstoque> atualizar(TipoSanguineo tipoSanguineo, int quantidadeBolsas, int capacidadeAlvo);

    /**
     * Lista as movimentacoes mais recentes, da mais nova para a mais antiga.
     *
     * @param limite quantidade maxima de registros
     * @return movimentacoes de todos os tipos sanguineos
     */
    List<MovimentacaoEstoque> listarMovimentacoes(int limite);

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
