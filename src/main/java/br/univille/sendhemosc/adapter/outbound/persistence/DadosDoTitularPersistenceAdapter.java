package br.univille.sendhemosc.adapter.outbound.persistence;

import br.univille.sendhemosc.domain.dto.DadosDoTitular;
import br.univille.sendhemosc.domain.port.outbound.IDadosDoTitularPort;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Le o historico do doador direto das tabelas. Sao consultas somente de leitura, feitas para
 * a exportacao: nao valia criar entidades e repositorios para cada tabela so para isso.
 */
@Component
@RequiredArgsConstructor
public class DadosDoTitularPersistenceAdapter implements IDadosDoTitularPort {

    private final JdbcTemplate jdbc;

    @Override
    @Transactional(readOnly = true)
    public List<DadosDoTitular.Consentimento> consentimentos(final Long doadorId) {
        return jdbc.query("""
                SELECT versao_termo, aceito, origem, registrado_em
                  FROM consentimento
                 WHERE doador_id = ?
                 ORDER BY registrado_em, id
                """,
                (linha, numero) -> new DadosDoTitular.Consentimento(
                        linha.getString("versao_termo"),
                        linha.getBoolean("aceito"),
                        linha.getString("origem"),
                        instante(linha.getTimestamp("registrado_em"))),
                doadorId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DadosDoTitular.Doacao> doacoes(final Long doadorId) {
        return jdbc.query("""
                SELECT data_doacao, local_coleta, observacao
                  FROM doacao
                 WHERE doador_id = ?
                 ORDER BY data_doacao, id
                """,
                (linha, numero) -> new DadosDoTitular.Doacao(
                        data(linha.getDate("data_doacao")),
                        linha.getString("local_coleta"),
                        linha.getString("observacao")),
                doadorId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DadosDoTitular.Convocacao> convocacoes(final Long doadorId) {
        return jdbc.query("""
                SELECT tipo_sanguineo_alvo, nivel_estoque, status, enviada_em, compareceu_em
                  FROM notificacao
                 WHERE doador_id = ?
                 ORDER BY criado_em, id
                """,
                (linha, numero) -> new DadosDoTitular.Convocacao(
                        linha.getString("tipo_sanguineo_alvo"),
                        linha.getString("nivel_estoque"),
                        linha.getString("status"),
                        instante(linha.getTimestamp("enviada_em")),
                        data(linha.getDate("compareceu_em"))),
                doadorId);
    }

    private static LocalDateTime instante(final Timestamp valor) {
        return valor == null ? null : valor.toLocalDateTime();
    }

    private static LocalDate data(final Date valor) {
        return valor == null ? null : valor.toLocalDate();
    }
}
