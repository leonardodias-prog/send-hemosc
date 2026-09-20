package br.univille.sendhemosc.domain.port.outbound;

import br.univille.sendhemosc.domain.dto.CandidatoConvocacao;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Porta de saida para consulta e persistencia de doadores. O dominio define o contrato;
 * o adapter de persistencia o implementa.
 */
public interface IDoadorRepositoryPort {

    /**
     * Busca doadores ativos, com consentimento de contato, cujo tipo sanguineo esteja entre os informados.
     * A filtragem por aptidao nao e feita aqui: o repositorio devolve os candidatos com os dados brutos
     * e quem decide a aptidao e CalcularAptidaoUseCase, para que a regra exista em um unico lugar.
     *
     * @param siglasTipoSanguineo siglas dos tipos sanguineos aceitos
     * @param referencia data de referencia da janela de doze meses
     * @return lista de candidatos a convocacao
     */
    List<CandidatoConvocacao> buscarCandidatos(Set<String> siglasTipoSanguineo, LocalDate referencia);

    /**
     * Revoga o consentimento de contato do doador a partir do token do link de descadastro.
     *
     * @param token token de descadastro
     * @return true se algum doador foi descadastrado
     */
    boolean descadastrarPorToken(String token);
}
