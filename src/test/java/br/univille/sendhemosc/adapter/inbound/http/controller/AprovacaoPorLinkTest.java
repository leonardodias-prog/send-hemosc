package br.univille.sendhemosc.adapter.inbound.http.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.UsuarioEntity;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.UsuarioJpaRepository;
import br.univille.sendhemosc.domain.enums.PerfilUsuario;
import br.univille.sendhemosc.domain.enums.SituacaoUsuario;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * O link de aprovacao do e-mail em dois passos: abrir mostra, o botao decide. Filtro de e-mail
 * que abre o link sozinho nao aprova ninguem.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Aprovacao pelo link do e-mail")
class AprovacaoPorLinkTest {

    private static final String TOKEN = "token-de-aprovacao-do-teste";
    private static final String LINK = "/aprovacao/" + TOKEN;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IUsuarioRepositoryPort usuarioRepository;

    @Autowired
    private UsuarioJpaRepository usuarioJpaRepository;

    private Long id;

    @BeforeEach
    void cadastrarResponsavel() {
        id = usuarioRepository.criar("Responsavel pelo Link", "responsavel.link@example.org", "hash",
                PerfilUsuario.RESPONSAVEL, SituacaoUsuario.PENDENTE, TOKEN);
    }

    private SituacaoUsuario situacao() {
        return usuarioRepository.buscarPorId(id).orElseThrow().situacao();
    }

    private void envelhecerCadastro(final int dias) {
        final UsuarioEntity entidade = usuarioJpaRepository.findById(id).orElseThrow();
        entidade.setCriadoEm(LocalDateTime.now().minusDays(dias));
        usuarioJpaRepository.save(entidade);
    }

    @Test
    @DisplayName("abrir o link de aprovar mostra o cadastro e o botao, e nao aprova nada")
    void abrirNaoAprova() throws Exception {
        mockMvc.perform(get(LINK + "/aprovar"))
                .andExpect(status().isOk())
                .andExpect(view().name("aprovacao-confirmacao"))
                .andExpect(content().string(containsString("Responsavel pelo Link")))
                .andExpect(content().string(containsString("Aprovar acesso")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));

        assertThat(situacao()).isEqualTo(SituacaoUsuario.PENDENTE);
    }

    @Test
    @DisplayName("o botao aprova, e o mesmo link nao decide de novo")
    void botaoAprova() throws Exception {
        mockMvc.perform(post(LINK + "/aprovar").with(csrf()))
                .andExpect(view().name("aprovacao"))
                .andExpect(content().string(containsString("Cadastro aprovado")));

        assertThat(situacao()).isEqualTo(SituacaoUsuario.ATIVO);

        mockMvc.perform(post(LINK + "/recusar").with(csrf()))
                .andExpect(content().string(containsString("Link já utilizado")));
        mockMvc.perform(get(LINK + "/aprovar"))
                .andExpect(view().name("aprovacao"))
                .andExpect(content().string(containsString("Link já utilizado")));
        assertThat(situacao()).isEqualTo(SituacaoUsuario.ATIVO);
    }

    @Test
    @DisplayName("recusar tambem passa pela confirmacao")
    void recusarConfirma() throws Exception {
        mockMvc.perform(get(LINK + "/recusar"))
                .andExpect(content().string(containsString("Recusar cadastro")));
        assertThat(situacao()).isEqualTo(SituacaoUsuario.PENDENTE);

        mockMvc.perform(post(LINK + "/recusar").with(csrf()))
                .andExpect(content().string(containsString("Cadastro recusado")));
        assertThat(situacao()).isEqualTo(SituacaoUsuario.RECUSADO);
    }

    @Test
    @DisplayName("sem o token de protecao do formulario, o botao nao vale")
    void semCsrf() throws Exception {
        mockMvc.perform(post(LINK + "/aprovar")).andExpect(status().isForbidden());

        assertThat(situacao()).isEqualTo(SituacaoUsuario.PENDENTE);
    }

    @Test
    @DisplayName("seis dias depois do cadastro, o link ainda vale")
    void aindaDentroDoPrazo() throws Exception {
        envelhecerCadastro(6);

        mockMvc.perform(get(LINK + "/aprovar"))
                .andExpect(view().name("aprovacao-confirmacao"));
    }

    @Test
    @DisplayName("link vencido nao decide, e a pagina manda para a tela de contas")
    void linkVencido() throws Exception {
        envelhecerCadastro(8);

        mockMvc.perform(get(LINK + "/aprovar"))
                .andExpect(view().name("aprovacao"))
                .andExpect(content().string(containsString("Link vencido")))
                .andExpect(content().string(containsString("tela de contas")));
        mockMvc.perform(post(LINK + "/aprovar").with(csrf()))
                .andExpect(content().string(containsString("Link vencido")));

        assertThat(situacao()).isEqualTo(SituacaoUsuario.PENDENTE);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "MASTER")
    @DisplayName("a tela de contas decide mesmo com o link vencido")
    void telaDeContasNaoDependeDoLink() throws Exception {
        envelhecerCadastro(30);

        mockMvc.perform(post("/usuarios/" + id + "/aprovar").with(csrf()));

        assertThat(situacao()).isEqualTo(SituacaoUsuario.ATIVO);
    }
}
