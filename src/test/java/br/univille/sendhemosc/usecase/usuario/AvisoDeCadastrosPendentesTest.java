package br.univille.sendhemosc.usecase.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import br.univille.sendhemosc.adapter.outbound.email.EnvioDeEmailRouter;
import br.univille.sendhemosc.domain.dto.MensagemEmail;
import br.univille.sendhemosc.domain.enums.PerfilUsuario;
import br.univille.sendhemosc.domain.enums.SituacaoUsuario;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * O aviso agrupado de cadastros pendentes contra o banco: a reserva por lote, o periodo entre
 * avisos e o teto da lista.
 *
 * <p>Os momentos ficam dez anos a frente: nenhum aviso gravado por outro teste cai dentro do
 * periodo, e cada teste controla o relogio que o aviso enxerga.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Aviso de cadastros pendentes")
class AvisoDeCadastrosPendentesTest {

    private static final String ADMINISTRADOR = "admin@sendhemosc.local";
    private static final LocalDateTime INICIO = LocalDateTime.now().plusYears(10).truncatedTo(ChronoUnit.SECONDS);

    @MockitoSpyBean
    private EnvioDeEmailRouter envioDeEmail;

    @Autowired
    private AvisarCadastrosPendentesUseCase avisarCadastrosPendentes;

    @Autowired
    private IUsuarioRepositoryPort usuarioRepository;

    @Autowired
    private MockMvc mockMvc;

    private String pendente(final String nome) {
        final String token = UUID.randomUUID().toString();
        usuarioRepository.criar(nome, nome.toLowerCase().replace(' ', '.') + "@example.org", "hash",
                PerfilUsuario.RESPONSAVEL, SituacaoUsuario.PENDENTE, token);

        return token;
    }

    private List<MensagemEmail> avisosAoAdministrador() {
        return Mockito.mockingDetails(envioDeEmail).getInvocations().stream()
                .filter(chamada -> chamada.getMethod().getName().equals("enviar"))
                .map(chamada -> (MensagemEmail) chamada.getArgument(0))
                .filter(mensagem -> mensagem.destinatario().equals(ADMINISTRADOR))
                .toList();
    }

    @Test
    @DisplayName("sai um aviso so, com os pendentes juntos, e os links abrem a pagina de confirmacao")
    void avisoAgrupado() {
        final String tokenAlfa = pendente("Pessoa Alfa");
        pendente("Pessoa Beta");

        assertThat(avisarCadastrosPendentes.executarSeDevido(INICIO)).isGreaterThanOrEqualTo(2);

        assertThat(avisosAoAdministrador()).singleElement().satisfies(aviso -> {
            assertThat(aviso.corpoHtml()).contains("Pessoa Alfa", "Pessoa Beta");
            assertThat(aviso.corpoHtml()).contains("/aprovacao/" + tokenAlfa + "/aprovar");
            assertThat(aviso.assunto()).contains("cadastros aguardando aprovacao");
        });
    }

    @Test
    @DisplayName("dentro do periodo nao sai outro aviso; o cadastro novo espera o seguinte")
    void periodoEntreAvisos() {
        pendente("Pessoa Alfa");
        avisarCadastrosPendentes.executarSeDevido(INICIO);
        pendente("Pessoa Gama");

        assertThat(avisarCadastrosPendentes.executarSeDevido(INICIO.plusMinutes(30))).isZero();
        assertThat(avisosAoAdministrador()).hasSize(1);

        assertThat(avisarCadastrosPendentes.executarSeDevido(INICIO.plusMinutes(61))).isEqualTo(1);
        assertThat(avisosAoAdministrador()).hasSize(2);
        assertThat(avisosAoAdministrador().getLast().corpoHtml()).contains("Pessoa Gama").doesNotContain("Pessoa Alfa");
    }

    @Test
    @DisplayName("quem ja entrou num aviso nao e avisado de novo")
    void semRepeticao() {
        pendente("Pessoa Alfa");
        avisarCadastrosPendentes.executarSeDevido(INICIO);

        assertThat(avisarCadastrosPendentes.executarSeDevido(INICIO.plusHours(2))).isZero();
        assertThat(avisosAoAdministrador()).hasSize(1);
    }

    @Test
    @DisplayName("a lista tem teto de dez; o resto aparece so como contagem")
    void tetoDaLista() {
        for (int i = 1; i <= 12; i++) {
            pendente("Pessoa Numero " + i);
        }

        avisarCadastrosPendentes.executarSeDevido(INICIO);

        assertThat(avisosAoAdministrador()).singleElement()
                .satisfies(aviso -> assertThat(aviso.corpoHtml()).contains("E mais"));
    }

    @Test
    @DisplayName("o cadastro de responsavel pela tela pede o aviso, sem precisar esperar o agendador")
    void cadastroPelaTelaAvisa() throws Exception {
        mockMvc.perform(post("/cadastro")
                .param("nome", "Pessoa da Tela")
                .param("email", "pessoa.da.tela@example.org")
                .param("perfil", "RESPONSAVEL")
                .param("senha", "senha-da-tela-123")
                .param("confirmacaoSenha", "senha-da-tela-123")
                .with(csrf()));

        assertThat(avisosAoAdministrador()).singleElement()
                .satisfies(aviso -> assertThat(aviso.corpoHtml()).contains("Pessoa da Tela"));
    }
}
