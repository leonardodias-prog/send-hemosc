package br.univille.sendhemosc.domain.port.outbound;

import br.univille.sendhemosc.domain.dto.CandidatoConvocacao;
import br.univille.sendhemosc.domain.dto.DoadorCadastrado;
import br.univille.sendhemosc.domain.dto.FiltroDoador;
import br.univille.sendhemosc.domain.dto.NovoDoador;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
     * Busca candidatos pelos criterios da tela de listagem. A aptidao nao e avaliada aqui:
     * o repositorio devolve os dados brutos e quem decide e CalcularAptidaoUseCase.
     *
     * @param filtro criterios informados
     * @param referencia data de referencia da janela de doze meses
     * @return candidatos que atendem aos criterios de busca e tipo
     */
    List<CandidatoConvocacao> buscarPorFiltro(FiltroDoador filtro, LocalDate referencia);

    /**
     * Busca candidatos por identificador, para a convocacao de uma selecao especifica.
     *
     * @param identificadores doadores escolhidos
     * @param referencia data de referencia da janela de doze meses
     * @return candidatos correspondentes, apenas os que existem
     */
    List<CandidatoConvocacao> buscarPorIdentificadores(Set<Long> identificadores, LocalDate referencia);

    /**
     * Indica se ja existe doador cadastrado com o e-mail informado.
     *
     * @param email endereco a verificar
     * @return true se o e-mail ja esta em uso
     */
    boolean existeComEmail(String email);

    /**
     * Persiste um novo doador, gerando o token usado no link de descadastro.
     *
     * @param doador dados do cadastro
     * @return identificador do doador criado
     */
    Long salvar(NovoDoador doador);

    /**
     * Total de doadores cadastrados.
     *
     * @return quantidade de doadores
     */
    long contar();

    /**
     * Revoga o consentimento de contato do doador a partir do token do link de descadastro.
     *
     * @param token token de descadastro
     * @return true se algum doador foi descadastrado
     */
    boolean descadastrarPorToken(String token);

    /**
     * Libera o limite de contato do doador: as convocacoes enviadas ate o instante informado
     * deixam de contar, tanto para o teto de convocacoes sem resposta quanto para o intervalo.
     *
     * @param id doador a liberar
     * @param quando instante da liberacao
     * @return true se o doador existia e estava ativo
     */
    boolean liberarContato(Long id, LocalDateTime quando);

    /**
     * Busca o cadastro completo, ativo ou nao.
     *
     * @param id doador
     * @return cadastro, se existir
     */
    Optional<DoadorCadastrado> buscarPorId(Long id);

    /**
     * Busca o cadastro pelo token do link enviado nos e-mails. E como o titular se identifica
     * sem ter conta no sistema.
     *
     * @param token token de descadastro
     * @return cadastro, se o token corresponder a algum doador
     */
    Optional<DoadorCadastrado> buscarPorToken(String token);

    /**
     * Lista os doadores desativados, que a busca normal nao mostra.
     *
     * @return cadastros desativados, por nome
     */
    List<DoadorCadastrado> listarInativos();

    /**
     * Indica se o e-mail ja pertence a outro doador, para a edicao nao colidir com um cadastro existente.
     *
     * @param email endereco a verificar
     * @param id doador em edicao, que pode manter o proprio e-mail
     * @return true se outro doador ja usa o e-mail
     */
    boolean existeComEmailEmOutro(String email, Long id);

    /**
     * Grava os dados editados.
     *
     * @param id doador
     * @param dados novos dados cadastrais
     */
    void atualizar(Long id, NovoDoador dados);

    /**
     * Ativa ou desativa o cadastro.
     *
     * @param id doador
     * @param ativo nova situacao
     * @return true se o doador existia
     */
    boolean definirAtivo(Long id, boolean ativo);

    /**
     * Remove o doador e, em cascata, as doacoes, convocacoes e consentimentos dele.
     *
     * @param id doador
     * @return true se o doador existia
     */
    boolean excluir(Long id);
}
