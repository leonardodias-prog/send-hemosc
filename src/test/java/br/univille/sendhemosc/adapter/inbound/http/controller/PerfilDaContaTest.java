package br.univille.sendhemosc.adapter.inbound.http.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.univille.sendhemosc.FotosDeTeste;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.AuditoriaJpaRepository;
import br.univille.sendhemosc.domain.dto.UsuarioResumo;
import br.univille.sendhemosc.domain.enums.PerfilUsuario;
import br.univille.sendhemosc.domain.enums.SituacaoUsuario;
import br.univille.sendhemosc.domain.port.outbound.IFotoUsuarioRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.IRecuperacaoSenhaPort;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Perfil da propria conta")
class PerfilDaContaTest {

    private static final String EMAIL = "perfil.proprio@example.org";
    private static final String NOME = "Pessoa do Perfil";
    private static final String SENHA = "senha-do-perfil-123";
    private static final String OUTRO_EMAIL = "outra.pessoa@example.org";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IUsuarioRepositoryPort usuarioRepository;

    @Autowired
    private IFotoUsuarioRepositoryPort fotoRepository;

    @Autowired
    private IRecuperacaoSenhaPort recuperacaoSenha;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditoriaJpaRepository auditoriaRepository;

    private Long id;

    @BeforeEach
    void criarConta() {
        id = usuarioRepository.criar(NOME, EMAIL, passwordEncoder.encode(SENHA),
                PerfilUsuario.OPERADOR, SituacaoUsuario.ATIVO, null);
    }

    private ResultActions enviarFoto(final byte[] arquivo) throws Exception {
        return mockMvc.perform(multipart("/conta/foto")
                .file(new MockMultipartFile("foto", "minha-foto.jpg", "image/jpeg", arquivo))
                .with(csrf()));
    }

    private ResultActions salvarDados(final String nome, final String email, final String senhaAtual) throws Exception {
        return mockMvc.perform(post("/conta/dados")
                .param("nome", nome)
                .param("email", email)
                .param("senhaAtual", senhaAtual)
                .with(csrf()));
    }

    private UsuarioResumo conta() {
        return usuarioRepository.buscarPorId(id).orElseThrow();
    }

    private boolean auditou(final String acao) {
        return auditoriaRepository.findAll().stream().anyMatch(registro -> registro.getAcao().equals(acao));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("a tela mostra todos os dados da conta")
    void mostraTodosOsDados() throws Exception {
        final String criadaEm = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        mockMvc.perform(get("/conta"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Meu perfil")))
                .andExpect(content().string(containsString(NOME)))
                .andExpect(content().string(containsString(EMAIL)))
                .andExpect(content().string(containsString("Operador")))
                .andExpect(content().string(containsString("Ativo")))
                .andExpect(content().string(containsString(criadaEm)))
                .andExpect(content().string(containsString("Este é o primeiro")));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("sem foto, aparece a inicial do nome, e nao ha imagem nem botao de remover")
    void semFotoMostraAInicial() throws Exception {
        mockMvc.perform(get("/conta"))
                .andExpect(content().string(containsString("avatar grande\" aria-hidden=\"true\">P</span>")))
                .andExpect(content().string(not(containsString("src=\"/conta/foto\""))))
                .andExpect(content().string(containsString("Escolher foto")))
                .andExpect(content().string(not(containsString("Remover foto"))));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("a foto enviada e guardada reduzida, e a tela passa a mostra-la no lugar da inicial")
    void enviarFotoGuardaEMostra() throws Exception {
        enviarFoto(FotosDeTeste.jpeg(3000, 2000))
                .andExpect(redirectedUrl("/conta"))
                .andExpect(flash().attribute("aviso", "Foto atualizada."));

        final var guardada = fotoRepository.buscar(id).orElseThrow();
        assertThat(guardada.tipoConteudo()).isEqualTo("image/jpeg");
        assertThat(FotosDeTeste.eJpeg(guardada.conteudo())).isTrue();
        assertThat(FotosDeTeste.ler(guardada.conteudo()).getWidth()).isEqualTo(256);

        mockMvc.perform(get("/conta"))
                .andExpect(content().string(containsString("src=\"/conta/foto\"")))
                .andExpect(content().string(not(containsString("aria-hidden=\"true\">P</span>"))))
                .andExpect(content().string(containsString("Trocar foto")))
                .andExpect(content().string(containsString("Remover foto")));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("a imagem e entregue como JPEG, so para a propria sessao")
    void entregaAFoto() throws Exception {
        enviarFoto(FotosDeTeste.png(600, 600));
        final byte[] guardada = fotoRepository.buscar(id).orElseThrow().conteudo();

        mockMvc.perform(get("/conta/foto"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(guardada));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("trocar a foto substitui a anterior: continua uma so por conta")
    void trocarSubstitui() throws Exception {
        enviarFoto(FotosDeTeste.jpeg(500, 500));
        final byte[] primeira = fotoRepository.buscar(id).orElseThrow().conteudo();

        enviarFoto(FotosDeTeste.gif(300, 300));

        assertThat(fotoRepository.buscar(id).orElseThrow().conteudo()).isNotEqualTo(primeira);
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("remover a foto devolve a inicial do nome")
    void removerVoltaAInicial() throws Exception {
        enviarFoto(FotosDeTeste.jpeg(500, 500));

        mockMvc.perform(post("/conta/foto/remover").with(csrf()))
                .andExpect(redirectedUrl("/conta"))
                .andExpect(flash().attribute("aviso", containsString("Foto removida")));

        assertThat(fotoRepository.buscar(id)).isEmpty();
        mockMvc.perform(get("/conta/foto")).andExpect(status().isNotFound());
        mockMvc.perform(get("/conta"))
                .andExpect(content().string(containsString("aria-hidden=\"true\">P</span>")));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("remover quando nao ha foto nao e erro")
    void removerSemFoto() throws Exception {
        mockMvc.perform(post("/conta/foto/remover").with(csrf()))
                .andExpect(redirectedUrl("/conta"));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("arquivo que nao e imagem e recusado, mesmo com nome e tipo de imagem")
    void arquivoQueNaoEImagem() throws Exception {
        enviarFoto("<?php echo 'nao sou uma foto'; ?>".getBytes(StandardCharsets.UTF_8))
                .andExpect(redirectedUrl("/conta"))
                .andExpect(flash().attribute("erro", containsString("Não foi possível usar esta imagem")));

        assertThat(fotoRepository.buscar(id)).isEmpty();
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("enviar sem escolher arquivo pede para escolher")
    void semArquivo() throws Exception {
        mockMvc.perform(multipart("/conta/foto").with(csrf()))
                .andExpect(redirectedUrl("/conta"))
                .andExpect(flash().attribute("erro", containsString("Escolha uma imagem")));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("arquivo de mais de 8 MB e recusado com a mensagem de tamanho")
    void arquivoGrandeDemais() throws Exception {
        enviarFoto(new byte[8 * 1024 * 1024 + 1])
                .andExpect(redirectedUrl("/conta"))
                .andExpect(flash().attribute("erro", containsString("passa de 8 MB")));

        assertThat(fotoRepository.buscar(id)).isEmpty();
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("a foto de outra conta nunca aparece: a rota nao recebe identificador")
    void fotoDeOutraContaNaoVaza() throws Exception {
        final Long outro = usuarioRepository.criar("Outra Pessoa", OUTRO_EMAIL, "hash",
                PerfilUsuario.OPERADOR, SituacaoUsuario.ATIVO, null);
        fotoRepository.salvar(outro, "image/jpeg", FotosDeTeste.jpeg(100, 100), LocalDateTime.now());

        mockMvc.perform(get("/conta/foto")).andExpect(status().isNotFound());
        mockMvc.perform(get("/conta/foto").param("id", String.valueOf(outro))).andExpect(status().isNotFound());
        mockMvc.perform(get("/conta/foto/" + outro)).andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("quem nao entrou nao ve foto nem envia: vai para a tela de entrada")
    void anonimoNaoAcessa() throws Exception {
        mockMvc.perform(get("/conta/foto")).andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(get("/conta")).andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(multipart("/conta/foto")
                        .file(new MockMultipartFile("foto", "a.jpg", "image/jpeg", FotosDeTeste.jpeg(50, 50)))
                        .with(csrf()))
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("sem o token CSRF o envio da foto, dos dados e da remocao e recusado")
    void exigeCsrf() throws Exception {
        mockMvc.perform(multipart("/conta/foto")
                        .file(new MockMultipartFile("foto", "a.jpg", "image/jpeg", FotosDeTeste.jpeg(50, 50))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/conta/foto/remover")).andExpect(status().isForbidden());
        mockMvc.perform(post("/conta/dados").param("nome", "X").param("email", EMAIL))
                .andExpect(status().isForbidden());
        assertThat(fotoRepository.buscar(id)).isEmpty();
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("corrigir so o nome nao pede senha, nao encerra a sessao e fica registrado")
    void corrigirONome() throws Exception {
        salvarDados("Pessoa Com Nome Certo", EMAIL, "")
                .andExpect(redirectedUrl("/conta"))
                .andExpect(flash().attribute("aviso", "Dados atualizados."));

        assertThat(conta().nome()).isEqualTo("Pessoa Com Nome Certo");
        assertThat(conta().email()).isEqualTo(EMAIL);
        assertThat(auditoriaRepository.findAll()).anyMatch(registro -> registro.getAcao().equals("PERFIL_ALTERADO")
                && registro.getDetalhe().contains("Pessoa Com Nome Certo"));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("espacos nas pontas do nome sao tirados")
    void nomeSemEspacosNasPontas() throws Exception {
        salvarDados("   Nome Aparado   ", EMAIL, "");

        assertThat(conta().nome()).isEqualTo("Nome Aparado");
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("salvar sem mexer em nada avisa e nao registra nada")
    void salvarSemMudar() throws Exception {
        salvarDados(NOME, EMAIL, "")
                .andExpect(flash().attribute("aviso", containsString("Nada a salvar")));

        assertThat(auditou("PERFIL_ALTERADO")).isFalse();
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("trocar o e-mail sem a senha atual e recusado, e o que foi digitado volta para o formulario")
    void trocarEmailSemSenha() throws Exception {
        salvarDados(NOME, "novo.endereco@example.org", "")
                .andExpect(redirectedUrl("/conta"))
                .andExpect(flash().attribute("erro", "A senha atual não confere."))
                .andExpect(flash().attribute("emailDigitado", "novo.endereco@example.org"))
                .andExpect(flash().attributeCount(3));

        assertThat(conta().email()).isEqualTo(EMAIL);
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("trocar o e-mail com a senha errada e recusado: sessao aberta nao basta")
    void trocarEmailComSenhaErrada() throws Exception {
        salvarDados(NOME, "novo.endereco@example.org", "nao-e-esta")
                .andExpect(flash().attribute("erro", "A senha atual não confere."));

        assertThat(conta().email()).isEqualTo(EMAIL);
        assertThat(auditou("PERFIL_ALTERADO")).isFalse();
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("trocar o e-mail com a senha certa grava, encerra a sessao e fecha o link de recuperacao antigo")
    void trocarEmailComSenhaCerta() throws Exception {
        final String hash = "b".repeat(64);
        final LocalDateTime agora = LocalDateTime.now();
        recuperacaoSenha.registrar(id, hash, agora, agora.plusMinutes(60));

        salvarDados(NOME, "  Novo.Endereco@Example.org ", SENHA)
                .andExpect(redirectedUrl("/login?emailAlterado"));

        assertThat(conta().email()).as("normalizado como no cadastro").isEqualTo("novo.endereco@example.org");
        assertThat(usuarioRepository.buscarPorEmail(EMAIL)).as("o endereco antigo deixou de entrar").isEmpty();
        assertThat(usuarioRepository.buscarPorEmail("novo.endereco@example.org")).isPresent();
        assertThat(recuperacaoSenha.buscarPorTokenHash(hash).orElseThrow().aberto(LocalDateTime.now())).isFalse();
        assertThat(auditoriaRepository.findAll()).anyMatch(registro -> registro.getAcao().equals("PERFIL_ALTERADO")
                && registro.getDetalhe().contains(EMAIL) && registro.getDetalhe().contains("novo.endereco@example.org"));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("e-mail que ja e de outra conta e recusado, mesmo com a senha certa")
    void emailDeOutraConta() throws Exception {
        usuarioRepository.criar("Outra Pessoa", OUTRO_EMAIL, "hash",
                PerfilUsuario.OPERADOR, SituacaoUsuario.ATIVO, null);

        salvarDados(NOME, OUTRO_EMAIL.toUpperCase(), SENHA)
                .andExpect(flash().attribute("erro", "Já existe uma conta com este e-mail."));

        assertThat(conta().email()).isEqualTo(EMAIL);
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("repetir o e-mail em outra caixa (maiuscula) nao conta como troca: nao pede senha nem encerra a sessao")
    void mesmoEmailEmOutraCaixa() throws Exception {
        salvarDados("Outro Nome", EMAIL.toUpperCase(), "")
                .andExpect(redirectedUrl("/conta"))
                .andExpect(flash().attribute("aviso", "Dados atualizados."));

        assertThat(conta().email()).isEqualTo(EMAIL);
        assertThat(conta().nome()).isEqualTo("Outro Nome");
    }

    @ParameterizedTest(name = "nome recusado: [{0}]")
    @ValueSource(strings = {"", "     ", "linha 1\nlinha 2", "tab\taqui", "x\u0000y"})
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("nome vazio ou com caractere de controle e recusado")
    void nomeInvalido(final String nome) throws Exception {
        salvarDados(nome, EMAIL, "")
                .andExpect(flash().attribute("erro", "Informe seu nome, com até 150 caracteres."));

        assertThat(conta().nome()).isEqualTo(NOME);
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("nome de 151 caracteres e recusado, o de 150 passa")
    void limiteDoNome() throws Exception {
        salvarDados("n".repeat(151), EMAIL, "")
                .andExpect(flash().attribute("erro", "Informe seu nome, com até 150 caracteres."));
        salvarDados("n".repeat(150), EMAIL, "")
                .andExpect(flash().attribute("aviso", "Dados atualizados."));

        assertThat(conta().nome()).hasSize(150);
    }

    @ParameterizedTest(name = "e-mail recusado: [{0}]")
    @ValueSource(strings = {"", "sem-arroba", "dois@@arrobas.org", "sem@ponto", "espaco no@meio.org", "@sem-usuario.org"})
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("e-mail mal formado e recusado")
    void emailInvalido(final String email) throws Exception {
        salvarDados(NOME, email, SENHA)
                .andExpect(flash().attribute("erro", "Informe um e-mail válido."));

        assertThat(conta().email()).isEqualTo(EMAIL);
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("e-mail de 181 caracteres e recusado")
    void emailLongoDemais() throws Exception {
        salvarDados(NOME, "u".repeat(181 - "@example.org".length()) + "@example.org", SENHA)
                .andExpect(flash().attribute("erro", "Informe um e-mail válido."));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("perfil de acesso e situacao nao mudam por aqui, mesmo que o pedido os traga")
    void perfilESituacaoNaoMudam() throws Exception {
        mockMvc.perform(post("/conta/dados")
                .param("nome", "Tentando Subir de Cargo")
                .param("email", EMAIL)
                .param("perfil", "MASTER")
                .param("situacao", "INATIVO")
                .with(csrf()));

        assertThat(conta().nome()).isEqualTo("Tentando Subir de Cargo");
        assertThat(conta().perfil()).isEqualTo(PerfilUsuario.OPERADOR);
        assertThat(conta().situacao()).isEqualTo(SituacaoUsuario.ATIVO);
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "OPERADOR")
    @DisplayName("nome e e-mail no tamanho maximo, antes e depois, passam de 500 e o registro e cortado em vez de falhar")
    void detalheDaAuditoriaCabe() throws Exception {
        final String emailMaximo = "e".repeat(180 - "@example.org".length()) + "@example.org";
        usuarioRepository.atualizarDadosPessoais(id, "a".repeat(150), EMAIL);

        salvarDados("n".repeat(150), emailMaximo, SENHA)
                .andExpect(redirectedUrl("/login?emailAlterado"));

        assertThat(auditoriaRepository.findAll())
                .filteredOn(registro -> registro.getAcao().equals("PERFIL_ALTERADO"))
                .singleElement()
                .satisfies(registro -> assertThat(registro.getDetalhe()).hasSize(500).endsWith("…"));
    }
}
