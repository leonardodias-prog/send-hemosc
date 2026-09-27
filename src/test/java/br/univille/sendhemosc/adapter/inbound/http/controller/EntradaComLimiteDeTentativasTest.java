package br.univille.sendhemosc.adapter.inbound.http.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.AuditoriaEntity;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.AuditoriaJpaRepository;
import br.univille.sendhemosc.domain.enums.PerfilUsuario;
import br.univille.sendhemosc.domain.enums.SituacaoUsuario;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * A entrada de ponta a ponta, com o limite de tentativas. O contador vive em memoria e e
 * compartilhado pelo contexto de teste inteiro, entao cada teste usa um e-mail so dele.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Entrada com limite de tentativas")
class EntradaComLimiteDeTentativasTest {

    private static final String SENHA = "senha-certa-123";
    private static final String SENHA_ERRADA = "chute-errado-999";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IUsuarioRepositoryPort usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditoriaJpaRepository auditoriaRepository;

    private String email;

    @BeforeEach
    void criarConta() {
        email = "entrada." + UUID.randomUUID() + "@example.org";
        usuarioRepository.criar("Pessoa da Entrada", email, passwordEncoder.encode(SENHA),
                PerfilUsuario.OPERADOR, SituacaoUsuario.ATIVO, null);
    }

    private ResultActions entrar(final String quem, final String senha) throws Exception {
        return mockMvc.perform(formLogin("/login").userParameter("email").passwordParam("senha")
                .user(quem).password(senha));
    }

    private void errar(final String quem, final int vezes) throws Exception {
        for (int i = 0; i < vezes; i++) {
            entrar(quem, SENHA_ERRADA);
        }
    }

    private List<AuditoriaEntity> auditoriaDe(final String quem) {
        return auditoriaRepository.findAll().stream()
                .filter(registro -> registro.getDetalhe() != null && registro.getDetalhe().contains(quem))
                .toList();
    }

    @Test
    @DisplayName("com a senha certa, entra")
    void senhaCertaEntra() throws Exception {
        entrar(email, SENHA).andExpect(authenticated()).andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("ate a quarta falha seguida, a mensagem e a de senha incorreta")
    void quatroFalhas() throws Exception {
        for (int i = 0; i < 4; i++) {
            entrar(email, SENHA_ERRADA).andExpect(unauthenticated()).andExpect(redirectedUrl("/login?erro"));
        }
    }

    @Test
    @DisplayName("a quinta falha bloqueia, e durante o bloqueio nem a senha certa entra")
    void quintaFalhaBloqueia() throws Exception {
        errar(email, 4);

        entrar(email, SENHA_ERRADA).andExpect(redirectedUrl("/login?bloqueado"));
        entrar(email, SENHA).andExpect(unauthenticated()).andExpect(redirectedUrl("/login?bloqueado"));
    }

    @Test
    @DisplayName("e-mail sem conta bloqueia do mesmo jeito: a mensagem nao revela quem tem cadastro")
    void emailSemContaBloqueiaIgual() throws Exception {
        final String inventado = "ninguem." + UUID.randomUUID() + "@example.org";

        errar(inventado, 4);

        entrar(inventado, SENHA_ERRADA).andExpect(redirectedUrl("/login?bloqueado"));
    }

    @Test
    @DisplayName("entrar com a senha certa zera a contagem")
    void acertoZera() throws Exception {
        errar(email, 4);
        entrar(email, SENHA).andExpect(authenticated());

        for (int i = 0; i < 4; i++) {
            entrar(email, SENHA_ERRADA).andExpect(redirectedUrl("/login?erro"));
        }
    }

    @Test
    @DisplayName("as falhas contra conta existente ficam na auditoria, sem a senha digitada")
    void auditoriaDaConta() throws Exception {
        errar(email, 5);

        final List<AuditoriaEntity> registros = auditoriaDe(email);

        assertThat(registros).filteredOn(registro -> registro.getAcao().equals("ENTRADA_RECUSADA"))
                .hasSize(5)
                .allMatch(registro -> registro.getDetalhe().contains("senha incorreta"));
        assertThat(registros).filteredOn(registro -> registro.getAcao().equals("ENTRADA_BLOQUEADA")).hasSize(1);
        assertThat(registros).noneMatch(registro -> registro.getDetalhe().contains(SENHA_ERRADA));
    }

    @Test
    @DisplayName("conta pendente recusada entra na auditoria com o motivo verdadeiro")
    void auditoriaDaContaPendente() throws Exception {
        final String pendente = "pendente." + UUID.randomUUID() + "@example.org";
        usuarioRepository.criar("Pessoa Pendente", pendente, passwordEncoder.encode(SENHA),
                PerfilUsuario.RESPONSAVEL, SituacaoUsuario.PENDENTE, UUID.randomUUID().toString());

        entrar(pendente, SENHA).andExpect(redirectedUrl("/login?erro"));

        assertThat(auditoriaDe(pendente)).singleElement()
                .satisfies(registro -> assertThat(registro.getDetalhe()).contains("conta em situacao PENDENTE"));
    }

    @Test
    @DisplayName("falha com e-mail sem conta nao grava nada: nao ha de quem ser rastro")
    void emailSemContaNaoGrava() throws Exception {
        final String inventado = "inventado." + UUID.randomUUID() + "@example.org";

        errar(inventado, 5);

        assertThat(auditoriaDe(inventado)).isEmpty();
    }

    @Test
    @DisplayName("a tela de entrada explica o bloqueio e aponta a recuperacao de senha")
    void telaExplicaBloqueio() throws Exception {
        mockMvc.perform(get("/login").param("bloqueado", ""))
                .andExpect(content().string(containsString("Muitas tentativas seguidas")))
                .andExpect(content().string(containsString("15")))
                .andExpect(content().string(containsString("Esqueci minha senha")));
    }
}
