package br.univille.sendhemosc.adapter.outbound.persistence.repository;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.DoadorEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
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
     * Retorna os candidatos a convocacao com os dados brutos necessarios a avaliacao de aptidao.
     * O filtro aqui e apenas de consentimento e tipo sanguineo; a regra de aptidao e aplicada no dominio.
     *
     * @param siglas siglas de tipo sanguineo aceitas
     * @param inicioJanela data inicial da janela de doze meses
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
                   (SELECT MAX(u.data_doacao) FROM doacao u WHERE u.doador_id = d.id) AS ultimaDoacao,
                   (SELECT COUNT(*) FROM doacao c WHERE c.doador_id = d.id AND c.data_doacao >= :inicioJanela) AS doacoesJanela
              FROM doador d
             WHERE d.ativo = TRUE
               AND d.aceita_contato = TRUE
               AND d.tipo_sanguineo IN (:siglas)
            """, nativeQuery = true)
    List<CandidatoProjection> buscarCandidatos(@Param("siglas") Set<String> siglas,
                                               @Param("inicioJanela") LocalDate inicioJanela);

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

        long getDoacoesJanela();
    }
}
