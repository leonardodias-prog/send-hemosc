package br.univille.sendhemosc.adapter.inbound.http.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.univille.sendhemosc.adapter.outbound.email.EnvioDeEmailRouter;
import br.univille.sendhemosc.adapter.outbound.persistence.entity.RecuperacaoSenhaEntity;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.RecuperacaoSenhaJpaRepository;
import br.univille.sendhemosc.domain.dto.MensagemEmail;
import br.univille.sendhemosc.domain.enums.PerfilUsuario;
import br.univille.sendhemosc.domain.enums.SituacaoUsuario;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * A recuperacao de senha de ponta a ponta: pedido, e-mail, link, senha nova e entrada com ela.
 * O envio fica em modo log; o espiao so observa a mensagem para tirar o link dela, como a
 * pessoa faria na caixa de entrada.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Recuperacao de senha")
class RecuperacaoDeSenhaTest {

    private static final String SENHA_ANTIGA = "senha-antiga-123";
    private static final String SENHA_NOVA = "senha-nova-456";
    private static final Pattern LINK = Pattern.compile("/senha/redefinir/([A-Za-z0-9_-]+)");

    @MockitoSpyBean
    private EnvioDeEmailRouter envioDeEmail;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IUsuarioRepositoryPort usuarioRepository;

    @Autowired
    private RecuperacaoSenhaJpaRepository recuperacaoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String email;
    private Long id;

    @BeforeEach
    void criarConta() {
        email = "recupera." + UUID.randomUUID() + "@example.org";
        id = usuarioRepository.criar("Pessoa Esquecida", email, passwordEncoder.encode(SENHA_ANTIGA),
                PerfilUsuario.OPERADOR, SituacaoUsuario.ATIVO, null);
    }

    private ResultActions pedir(final String quem) throws Exception {
        return mockMvc.perform(post("/senha/esqueci").param("email", quem).with(csrf()));
    }

    private List<MensagemEmail> enviadas() {
        return Mockito.mockingDetails(envioDeEmail).getInvocations().stream()
                .filter(chamada -> chamada.getMethod().getName().equals("enviar"))
                .map(chamada -> (MensagemEmail) chamada.getArgument(0))
                .toList();
    }

    /** Pede o link e o tira do e-mail, como a pessoa faria. */
    private String pedirEAbrirOEmail() throws Exception {
        pedir(email);

        final MensagemEmail mensagem = enviadas().getLast();
        final Matcher link = LINK.matcher(mensagem.corpoHtml());

        assertThat(mensagem.destinatario()).isEqualTo(email);
        assertThat(link.find()).as("o e-mail traz o link").isTrue();

        return link.group(1);
    }

    private ResultActions redefinir(final String token, final String nova, final String confirmacao) throws Exception {
        return mockMvc.perform(post("/senha/redefinir/" + token)
                .param("novaSenha", nova)
                .param("confirmacao", confirmacao)
                .with(csrf()));
    }

    private ResultActions entrar(final String senha) throws Exception {
        return mockMvc.perform(formLogin("/login").userParameter("email").passwordParam("senha")
                .user(email).password(senha));
    }

    @Test
    @DisplayName("a tela de pedido e publica e diz por quanto tempo o link vale")
    void telaDePedido() throws Exception {
        mockMvc.perform(get("/senha/esqueci"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("60</span> minutos")));
    }

    @Test
    @DisplayName("conta ativa recebe o link; e-mail sem conta, nada, com a mesma resposta na tela")
    void mesmaRespostaComOuSemConta() throws Exception {
        pedir(email)
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("aviso", containsString("Se houver uma conta ativa")));
        pedir("ninguem." + UUID.randomUUID() + "@example.org")
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("aviso", containsString("Se houver uma conta ativa")));

        assertThat(enviadas()).singleElement()
                .satisfies(mensagem -> assertThat(mensagem.destinatario()).isEqualTo(email));
    }

    @Test
    @DisplayName("pedir de novo logo em seguida nao manda outro e-mail")
    void pedidoRepetido() throws Exception {
        pedir(email);
        pedir(email);

        assertThat(enviadas()).hasSize(1);
    }

    @Test
    @DisplayName("o banco guarda o hash do token, nunca o token que vai no link")
    void bancoGuardaSoOHash() throws Exception {
        final String token = pedirEAbrirOEmail();
        final List<RecuperacaoSenhaEntity> pedidos = recuperacaoRepository.findAll().stream()
                .filter(pedido -> pedido.getUsuarioId().equals(id))
                .toList();

        assertThat(pedidos).singleElement().satisfies(pedido -> {
            assertThat(pedido.getTokenHash()).isNotEqualTo(token);
            assertThat(pedido.getTokenHash()).isEqualTo(sha256(token));
        });
    }

    @Test
    @DisplayName("abrir o link mostra o formulario e nao gasta o link")
    void abrirNaoGasta() throws Exception {
        final String token = pedirEAbrirOEmail();

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(get("/senha/redefinir/" + token))
                    .andExpect(content().string(containsString("Criar senha nova")))
                    .andExpect(content().string(containsString(email)));
        }
    }

    @Test
    @DisplayName("com o link, a senha nova passa a valer, a antiga nao, e o link nao serve de novo")
    void senhaNovaPeloLink() throws Exception {
        final String token = pedirEAbrirOEmail();

        redefinir(token, SENHA_NOVA, SENHA_NOVA)
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("aviso", containsString("Senha nova criada")));

        entrar(SENHA_NOVA).andExpect(authenticated());
        entrar(SENHA_ANTIGA).andExpect(unauthenticated());
        mockMvc.perform(get("/senha/redefinir/" + token))
                .andExpect(content().string(containsString("Link sem validade")))
                .andExpect(content().string(not(containsString("name=\"novaSenha\""))));
        redefinir(token, "outra-senha-789", "outra-senha-789").andExpect(redirectedUrl("/senha/redefinir/" + token));
        entrar("outra-senha-789").andExpect(unauthenticated());
    }

    @Test
    @DisplayName("erro na senha nova volta ao formulario com o link ainda valido")
    void erroNaoGastaOLink() throws Exception {
        final String token = pedirEAbrirOEmail();

        redefinir(token, "curta", "curta")
                .andExpect(redirectedUrl("/senha/redefinir/" + token))
                .andExpect(flash().attribute("erro", containsString("entre 8 e 72")));

        mockMvc.perform(get("/senha/redefinir/" + token))
                .andExpect(content().string(containsString("name=\"novaSenha\"")));
    }

    @Test
    @DisplayName("link vencido nao serve")
    void linkVencido() throws Exception {
        final String token = pedirEAbrirOEmail();
        final RecuperacaoSenhaEntity pedido = recuperacaoRepository.findByTokenHash(sha256(token)).orElseThrow();
        pedido.setExpiraEm(LocalDateTime.now().minusMinutes(1));
        recuperacaoRepository.save(pedido);

        mockMvc.perform(get("/senha/redefinir/" + token))
                .andExpect(content().string(containsString("Link sem validade")));
        redefinir(token, SENHA_NOVA, SENHA_NOVA);
        entrar(SENHA_NOVA).andExpect(unauthenticated());
    }

    @Test
    @DisplayName("quem estava bloqueado por errar a senha entra depois de redefinir pelo link")
    void redefinirDesbloqueia() throws Exception {
        for (int i = 0; i < 5; i++) {
            entrar("chute-" + i);
        }
        entrar(SENHA_ANTIGA).andExpect(redirectedUrl("/login?bloqueado"));

        redefinir(pedirEAbrirOEmail(), SENHA_NOVA, SENHA_NOVA);

        entrar(SENHA_NOVA).andExpect(authenticated());
    }

    @Test
    @DisplayName("sem o token de protecao do formulario, o pedido nao vale")
    void semCsrf() throws Exception {
        mockMvc.perform(post("/senha/esqueci").param("email", email)).andExpect(status().isForbidden());

        assertThat(enviadas()).isEmpty();
    }

    private static String sha256(final String valor) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(valor.getBytes(StandardCharsets.UTF_8)));
    }
}
