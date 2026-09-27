package br.univille.sendhemosc.adapter.inbound.http.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;

import br.univille.sendhemosc.adapter.outbound.persistence.repository.AuditoriaJpaRepository;
import br.univille.sendhemosc.domain.enums.PerfilUsuario;
import br.univille.sendhemosc.domain.enums.SituacaoUsuario;
import br.univille.sendhemosc.domain.port.outbound.IRecuperacaoSenhaPort;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Troca da propria senha")
class TrocaDaPropriaSenhaTest {

    private static final String EMAIL = "troca.senha@example.org";
    private static final String SENHA_ATUAL = "senha-atual-123";
    private static final String SENHA_NOVA = "senha-nova-456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IUsuarioRepositoryPort usuarioRepository;

    @Autowired
    private IRecuperacaoSenhaPort recuperacaoSenha;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditoriaJpaRepository auditoriaRepository;

    private Long id;

    @BeforeEach
    void criarConta() {
        id = usuarioRepository.criar("Pessoa que Troca", EMAIL, passwordEncoder.encode(SENHA_ATUAL),
                PerfilUsuario.OPERADOR, SituacaoUsuario.ATIVO, null);
    }

    private String hashGravado() {
        return usuarioRepository.buscarPorEmail(EMAIL).orElseThrow().senhaHash();
    }

    private ResultActions trocar(final String atual, final String nova, final String confirmacao) throws Exception {
        return mockMvc.perform(post("/conta/senha")
                .param("senhaAtual", atual)
                .param("novaSenha", nova)
                .param("confirmacao", confirmacao)
                .with(csrf()));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("com a senha atual certa, a nova passa a valer e a antiga deixa de valer")
    void trocaComSenhaAtualCerta() throws Exception {
        trocar(SENHA_ATUAL, SENHA_NOVA, SENHA_NOVA)
                .andExpect(redirectedUrl("/conta/senha"))
                .andExpect(flash().attribute("aviso", containsString("Senha alterada")));

        assertThat(passwordEncoder.matches(SENHA_NOVA, hashGravado())).isTrue();
        assertThat(passwordEncoder.matches(SENHA_ATUAL, hashGravado())).isFalse();
        assertThat(auditoriaRepository.findAll())
                .anyMatch(registro -> registro.getAcao().equals("SENHA_ALTERADA") && registro.getDetalhe().contains(EMAIL));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("senha atual errada nao muda nada: sessao aberta nao basta para trocar a senha")
    void senhaAtualErrada() throws Exception {
        trocar("nao-e-esta", SENHA_NOVA, SENHA_NOVA)
                .andExpect(flash().attribute("erro", "A senha atual não confere."));

        assertThat(passwordEncoder.matches(SENHA_ATUAL, hashGravado())).isTrue();
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("senha nova curta e recusada")
    void senhaNovaCurta() throws Exception {
        trocar(SENHA_ATUAL, "curta", "curta")
                .andExpect(flash().attribute("erro", containsString("entre 8 e 72")));

        assertThat(passwordEncoder.matches(SENHA_ATUAL, hashGravado())).isTrue();
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("repeticao diferente e recusada")
    void repeticaoDiferente() throws Exception {
        trocar(SENHA_ATUAL, SENHA_NOVA, "outra-coisa-789")
                .andExpect(flash().attribute("erro", containsString("A confirmação não confere")));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("mais de 72 bytes e recusado mesmo com menos de 72 letras: e o limite do BCrypt")
    void limiteEmBytes() throws Exception {
        final String acentuada = "á".repeat(40);

        trocar(SENHA_ATUAL, acentuada, acentuada)
                .andExpect(flash().attribute("erro", containsString("entre 8 e 72")));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("trocar a senha fecha o link de recuperacao que estava aberto")
    void fechaLinkDeRecuperacao() throws Exception {
        final String hash = "a".repeat(64);
        final LocalDateTime agora = LocalDateTime.now();
        recuperacaoSenha.registrar(id, hash, agora, agora.plusMinutes(60));

        trocar(SENHA_ATUAL, SENHA_NOVA, SENHA_NOVA);

        assertThat(recuperacaoSenha.buscarPorTokenHash(hash).orElseThrow().aberto(LocalDateTime.now())).isFalse();
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("a tela tem a casca, com Minha senha marcada, e nao devolve senha nenhuma")
    void telaComCasca() throws Exception {
        mockMvc.perform(get("/conta/senha"))
                .andExpect(content().string(containsString("class=\"lateral\"")))
                .andExpect(content().string(containsString("Minha senha")))
                .andExpect(content().string(containsString("aria-current=\"page\"")))
                .andExpect(content().string(containsString("autocomplete=\"current-password\"")));
    }

    @Test
    @DisplayName("quem nao entrou e mandado para a tela de entrada")
    void anonimoVaiParaEntrada() throws Exception {
        mockMvc.perform(get("/conta/senha")).andExpect(redirectedUrlPattern("**/login"));
    }
}
