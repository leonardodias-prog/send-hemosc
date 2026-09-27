package br.univille.sendhemosc;

import static org.assertj.core.api.Assertions.assertThat;

import br.univille.sendhemosc.domain.dto.CadastroPendente;
import br.univille.sendhemosc.domain.dto.FiltroDoador;
import br.univille.sendhemosc.domain.dto.SituacaoEstoque;
import br.univille.sendhemosc.domain.enums.PerfilUsuario;
import br.univille.sendhemosc.domain.enums.SituacaoUsuario;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.port.outbound.IDisparoAutomaticoRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.IRecuperacaoSenhaPort;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import br.univille.sendhemosc.usecase.estoque.ListarSituacaoEstoqueUseCase;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

    @Autowired
    private IRecuperacaoSenhaPort recuperacaoSenha;

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
                "usuario", "consentimento", "auditoria", "disparo_automatico", "recuperacao_senha");
    }

    @Test
    @DisplayName("o aviso de cadastros encontra no PostgreSQL exatamente o lote que marcou")
    void avisoDeCadastrosNoPostgres() {
        final String sufixo = UUID.randomUUID().toString();
        final String email = "pendente." + sufixo + "@example.org";
        usuarioRepository.criar("Pendente " + sufixo, email, "hash",
                PerfilUsuario.RESPONSAVEL, SituacaoUsuario.PENDENTE, "token-" + sufixo);

        // A busca do lote compara o momento gravado com o procurado: precisao de timestamp
        // diferente entre Java e o banco faria o lote voltar vazio.
        final List<CadastroPendente> lote = usuarioRepository.reservarPendentesParaAviso(LocalDateTime.now());

        assertThat(lote).extracting(CadastroPendente::email).contains(email);
        assertThat(usuarioRepository.ultimoAvisoDeCadastros()).isPresent();
    }

    @Test
    @DisplayName("os limites e o uso unico do link de recuperacao rodam no PostgreSQL")
    void recuperacaoDeSenhaNoPostgres() {
        final String sufixo = UUID.randomUUID().toString().replace("-", "");
        final Long id = usuarioRepository.criar("Conta " + sufixo, "conta." + sufixo + "@example.org", "hash",
                PerfilUsuario.OPERADOR, SituacaoUsuario.ATIVO, null);
        final LocalDateTime agora = LocalDateTime.now();
        final String hash = sufixo + sufixo;

        recuperacaoSenha.registrar(id, hash, agora, agora.plusMinutes(60));
        final Long pedido = recuperacaoSenha.buscarPorTokenHash(hash).orElseThrow().id();

        assertThat(recuperacaoSenha.contarDaConta(id, agora.minusDays(1))).isEqualTo(1);
        assertThat(recuperacaoSenha.contarTodos(agora.minusDays(1))).isPositive();
        assertThat(recuperacaoSenha.ultimoDaConta(id)).isPresent();
        assertThat(recuperacaoSenha.usar(pedido, agora.plusMinutes(1))).isTrue();
        assertThat(recuperacaoSenha.usar(pedido, agora.plusMinutes(2))).isFalse();
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
