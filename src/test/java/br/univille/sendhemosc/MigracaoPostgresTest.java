package br.univille.sendhemosc;

import static org.assertj.core.api.Assertions.assertThat;

import br.univille.sendhemosc.domain.dto.FiltroDoador;
import br.univille.sendhemosc.domain.dto.SituacaoEstoque;
import br.univille.sendhemosc.domain.enums.PerfilUsuario;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.port.outbound.IDisparoAutomaticoRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import br.univille.sendhemosc.usecase.estoque.ListarSituacaoEstoqueUseCase;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Executa as migrations contra um PostgreSQL de verdade.
 *
 * <p>Os demais testes usam H2 em modo de compatibilidade, que aceita construcoes que o
 * PostgreSQL recusaria. Como o ambiente publicado roda PostgreSQL, diferenca de dialeto so
 * apareceria no deploy, com a aplicacao ja no ar e sem subir.</p>
 *
 * <p>Este teste so roda quando a variavel DB_URL_TESTE existe, o que acontece na integracao
 * continua, onde um PostgreSQL e disponibilizado. Na maquina de desenvolvimento ele e
 * ignorado, sem exigir banco instalado.</p>
 */
@SpringBootTest
@ActiveProfiles("producao")
@EnabledIfEnvironmentVariable(named = "DB_URL_TESTE", matches = ".+")
@DisplayName("Migrations em PostgreSQL real")
class MigracaoPostgresTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ListarSituacaoEstoqueUseCase listarSituacaoEstoque;

    @Autowired
    private IUsuarioRepositoryPort usuarioRepository;

    @Autowired
    private IDoadorRepositoryPort doadorRepository;

    @Autowired
    private IDisparoAutomaticoRepositoryPort disparoRepository;

    @Test
    @DisplayName("o banco realmente e PostgreSQL, e nao H2 por engano")
    void bancoEPostgres() throws Exception {
        try (Connection conexao = dataSource.getConnection()) {
            final DatabaseMetaData metadados = conexao.getMetaData();

            assertThat(metadados.getDatabaseProductName()).isEqualTo("PostgreSQL");
        }
    }

    @Test
    @DisplayName("todas as tabelas foram criadas pelas migrations")
    void tabelasCriadas() throws Exception {
        final List<String> tabelas = new ArrayList<>();

        try (Connection conexao = dataSource.getConnection();
             ResultSet resultado = conexao.getMetaData()
                     .getTables(null, "public", "%", new String[] {"TABLE"})) {
            while (resultado.next()) {
                tabelas.add(resultado.getString("TABLE_NAME").toLowerCase());
            }
        }

        assertThat(tabelas).contains(
                "doador", "doacao", "estoque_hemocomponente", "notificacao",
                "usuario", "consentimento", "auditoria", "disparo_automatico");
    }

    @Test
    @DisplayName("o estoque inicial dos oito tipos foi carregado")
    void estoqueInicialCarregado() {
        final List<SituacaoEstoque> situacoes = listarSituacaoEstoque.execute();

        assertThat(situacoes).hasSize(8);
    }

    @Test
    @DisplayName("o administrador inicial foi criado na subida")
    void administradorCriado() {
        assertThat(usuarioRepository.existeAlgumComPerfil(PerfilUsuario.MASTER)).isTrue();
    }

    @Test
    @DisplayName("a listagem de contas funciona no PostgreSQL")
    void listagemDeContasFunciona() {
        assertThat(usuarioRepository.listarTodos()).isNotNull();
    }

    @Test
    @DisplayName("as consultas nativas de candidatos, com o historico de contato, rodam no PostgreSQL")
    void consultasDeCandidatosFuncionam() {
        final LocalDate hoje = LocalDate.now();

        assertThat(doadorRepository.buscarCandidatos(TipoSanguineo.A_POSITIVO.siglasDoadoresCompativeis(), hoje))
                .isNotNull();
        assertThat(doadorRepository.buscarPorFiltro(FiltroDoador.vazio(), hoje)).isNotNull();
    }

    @Test
    @DisplayName("o disparo automatico nasce desligado")
    void disparoNasceDesligado() {
        assertThat(disparoRepository.buscar().ativo()).isFalse();
    }
}
