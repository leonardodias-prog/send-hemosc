package br.univille.sendhemosc.adapter.outbound.persistence.repository;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.DoadorEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acesso JPA a tabela de doadores.
 */
public interface DoadorJpaRepository extends JpaRepository<DoadorEntity, Long> {

    /**
     * Retorna os candidatos a convocacao com os dados brutos necessarios a avaliacao de aptidao e
     * dos limites de contato. O filtro aqui e apenas de consentimento e tipo sanguineo; as duas
     * regras sao aplicadas no dominio.
     *
     * @param siglas siglas de tipo sanguineo aceitas
     * @param inicioJanela data inicial da janela de doze meses
     * @param inicioPrazoTeto convocacoes sem resposta anteriores a este instante nao contam para o teto
     * @return projecoes dos candidatos
     */
    @Query(value = """
            SELECT d.id                AS id,
                   d.nome              AS nome,
                   d.email             AS email,
                   d.tipo_sanguineo    AS tipoSanguineo,
                   d.token_descadastro AS tokenDescadastro,
                   d.sexo              AS sexo,
                   d.data_nascimento   AS dataNascimento,
                   d.peso_kg           AS pesoKg,
                   d.aceita_contato    AS aceitaContato,
                   (SELECT MAX(u.data_doacao) FROM doacao u WHERE u.doador_id = d.id) AS ultimaDoacao,
                   (SELECT COUNT(*) FROM doacao c WHERE c.doador_id = d.id AND c.data_doacao >= :inicioJanela) AS doacoesJanela,
                   (SELECT COUNT(*) FROM notificacao p
                     WHERE p.doador_id = d.id AND p.status = 'ENVIADA' AND p.compareceu_em IS NULL
                       AND p.enviada_em >= :inicioPrazoTeto
                       AND (d.contato_liberado_em IS NULL OR p.enviada_em > d.contato_liberado_em)) AS convocacoesSemResposta,
                   (SELECT MAX(e.enviada_em) FROM notificacao e
                     WHERE e.doador_id = d.id
                       AND (d.contato_liberado_em IS NULL OR e.enviada_em > d.contato_liberado_em)) AS ultimaConvocacao
              FROM doador d
             WHERE d.ativo = TRUE
               AND d.aceita_contato = TRUE
               AND d.tipo_sanguineo IN (:siglas)
            """, nativeQuery = true)
    List<CandidatoProjection> buscarCandidatos(@Param("siglas") Set<String> siglas,
                                               @Param("inicioJanela") LocalDate inicioJanela,
                                               @Param("inicioPrazoTeto") LocalDateTime inicioPrazoTeto);

    /**
     * Candidatos que atendem aos criterios da tela de listagem. O filtro de aptidao nao entra
     * aqui: ele depende de regra de dominio e e aplicado depois, em um unico lugar.
     *
     * @param padraoBusca trecho de nome ou e-mail entre curingas, ou apenas curinga
     * @param sigla sigla do tipo sanguineo, ou nulo para todos
     * @param somenteComConsentimento quando true, descarta quem nao autorizou contato
     * @param inicioJanela data inicial da janela de doze meses
     * @param inicioPrazoTeto convocacoes sem resposta anteriores a este instante nao contam para o teto
     * @return projecoes dos candidatos
     */
    @Query(value = """
            SELECT d.id                AS id,
                   d.nome              AS nome,
                   d.email             AS email,
                   d.tipo_sanguineo    AS tipoSanguineo,
                   d.token_descadastro AS tokenDescadastro,
                   d.sexo              AS sexo,
                   d.data_nascimento   AS dataNascimento,
                   d.peso_kg           AS pesoKg,
                   d.aceita_contato    AS aceitaContato,
                   (SELECT MAX(u.data_doacao) FROM doacao u WHERE u.doador_id = d.id) AS ultimaDoacao,
                   (SELECT COUNT(*) FROM doacao c WHERE c.doador_id = d.id AND c.data_doacao >= :inicioJanela) AS doacoesJanela,
                   (SELECT COUNT(*) FROM notificacao p
                     WHERE p.doador_id = d.id AND p.status = 'ENVIADA' AND p.compareceu_em IS NULL
                       AND p.enviada_em >= :inicioPrazoTeto
                       AND (d.contato_liberado_em IS NULL OR p.enviada_em > d.contato_liberado_em)) AS convocacoesSemResposta,
                   (SELECT MAX(e.enviada_em) FROM notificacao e
                     WHERE e.doador_id = d.id
                       AND (d.contato_liberado_em IS NULL OR e.enviada_em > d.contato_liberado_em)) AS ultimaConvocacao
              FROM doador d
             WHERE d.ativo = TRUE
               AND (:somenteComConsentimento = FALSE OR d.aceita_contato = TRUE)
               AND (CAST(:sigla AS VARCHAR) IS NULL OR d.tipo_sanguineo = :sigla)
               AND (LOWER(d.nome) LIKE :padraoBusca OR LOWER(d.email) LIKE :padraoBusca)
             ORDER BY d.nome
            """, nativeQuery = true)
    List<CandidatoProjection> buscarPorFiltro(@Param("padraoBusca") String padraoBusca,
                                              @Param("sigla") String sigla,
                                              @Param("somenteComConsentimento") boolean somenteComConsentimento,
                                              @Param("inicioJanela") LocalDate inicioJanela,
                                              @Param("inicioPrazoTeto") LocalDateTime inicioPrazoTeto);

    /**
     * Candidatos escolhidos por identificador.
     *
     * @param identificadores doadores selecionados
     * @param inicioJanela data inicial da janela de doze meses
     * @param inicioPrazoTeto convocacoes sem resposta anteriores a este instante nao contam para o teto
     * @return projecoes dos candidatos
     */
    @Query(value = """
            SELECT d.id                AS id,
                   d.nome              AS nome,
                   d.email             AS email,
                   d.tipo_sanguineo    AS tipoSanguineo,
                   d.token_descadastro AS tokenDescadastro,
                   d.sexo              AS sexo,
                   d.data_nascimento   AS dataNascimento,
                   d.peso_kg           AS pesoKg,
                   d.aceita_contato    AS aceitaContato,
                   (SELECT MAX(u.data_doacao) FROM doacao u WHERE u.doador_id = d.id) AS ultimaDoacao,
                   (SELECT COUNT(*) FROM doacao c WHERE c.doador_id = d.id AND c.data_doacao >= :inicioJanela) AS doacoesJanela,
                   (SELECT COUNT(*) FROM notificacao p
                     WHERE p.doador_id = d.id AND p.status = 'ENVIADA' AND p.compareceu_em IS NULL
                       AND p.enviada_em >= :inicioPrazoTeto
                       AND (d.contato_liberado_em IS NULL OR p.enviada_em > d.contato_liberado_em)) AS convocacoesSemResposta,
                   (SELECT MAX(e.enviada_em) FROM notificacao e
                     WHERE e.doador_id = d.id
                       AND (d.contato_liberado_em IS NULL OR e.enviada_em > d.contato_liberado_em)) AS ultimaConvocacao
              FROM doador d
             WHERE d.ativo = TRUE
               AND d.id IN (:identificadores)
             ORDER BY d.nome
            """, nativeQuery = true)
    List<CandidatoProjection> buscarPorIdentificadores(@Param("identificadores") Set<Long> identificadores,
                                                       @Param("inicioJanela") LocalDate inicioJanela,
                                                       @Param("inicioPrazoTeto") LocalDateTime inicioPrazoTeto);

    /**
     * Revoga o consentimento de contato a partir do token de descadastro.
     *
     * @param token token recebido no link do e-mail
     * @return quantidade de registros afetados
     */
    @Modifying
    @Query("UPDATE DoadorEntity d SET d.aceitaContato = false, d.atualizadoEm = CURRENT_TIMESTAMP "
            + "WHERE d.tokenDescadastro = :token AND d.aceitaContato = true")
    int descadastrarPorToken(@Param("token") String token);

    /**
     * Libera o limite de contato: convocacoes enviadas ate agora deixam de contar.
     *
     * @param id doador a liberar
     * @param quando instante da liberacao
     * @return quantidade de registros afetados
     */
    @Modifying
    @Query("UPDATE DoadorEntity d SET d.contatoLiberadoEm = :quando, d.atualizadoEm = :quando "
            + "WHERE d.id = :id AND d.ativo = true")
    int liberarContato(@Param("id") Long id, @Param("quando") LocalDateTime quando);

    boolean existsByEmail(String email);

    /**
     * Projecao usada pela consulta de candidatos.
     */
    interface CandidatoProjection {

        Long getId();

        String getNome();

        String getEmail();

        String getTipoSanguineo();

        String getTokenDescadastro();

        String getSexo();

        LocalDate getDataNascimento();

        BigDecimal getPesoKg();

        LocalDate getUltimaDoacao();

        boolean getAceitaContato();

        long getDoacoesJanela();

        long getConvocacoesSemResposta();

        LocalDateTime getUltimaConvocacao();
    }
}
