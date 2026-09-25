package br.univille.sendhemosc.adapter.inbound.http.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import br.univille.sendhemosc.adapter.outbound.persistence.repository.DoadorJpaRepository;
import br.univille.sendhemosc.domain.dto.DoadorCadastrado;
import br.univille.sendhemosc.domain.dto.DoadorListado;
import br.univille.sendhemosc.domain.dto.NovaDoacao;
import br.univille.sendhemosc.domain.dto.NovoDoador;
import br.univille.sendhemosc.domain.enums.Sexo;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.port.outbound.IDoacaoRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Manutencao do cadastro pela equipe e os direitos do titular pelo link dos e-mails,
 * de ponta a ponta: tela, regra de acesso e banco.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Manutencao do cadastro de doador")
class CadastroDoadorViewAdapterTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private IDoadorRepositoryPort doadorRepository;
    @Autowired
    private IDoacaoRepositoryPort doacaoRepository;
    @Autowired
    private DoadorJpaRepository doadorJpa;

    private Long id;
    private String email;

    @BeforeEach
    void cadastrar() {
        email = "teste-" + UUID.randomUUID() + "@example.org";
        id = doadorRepository.salvar(new NovoDoador("Doador de Teste", email, null, TipoSanguineo.B_NEGATIVO,
                Sexo.MASCULINO, LocalDate.of(1990, 1, 1), null, true));
    }

    private String token() {
        return doadorJpa.findById(id).orElseThrow().getTokenDescadastro();
    }

    @Nested
    @DisplayName("como operador")
    @WithMockUser(username = "operador@example.org", roles = "OPERADOR")
    class ComoOperador {

        @Test
        @DisplayName("abre a edicao com os dados atuais")
        void abreEdicao() throws Exception {
            mockMvc.perform(get("/doadores/" + id + "/editar"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("doador-edicao"))
                    .andExpect(content().string(Matchers.containsString(email)));
        }

        @Test
        @DisplayName("edita o cadastro")
        void edita() throws Exception {
            mockMvc.perform(post("/doadores/" + id + "/editar").with(csrf())
                            .param("nome", "Nome Corrigido")
                            .param("email", email)
                            .param("tipoSanguineo", "O_NEGATIVO")
                            .param("sexo", "MASCULINO")
                            .param("dataNascimento", "1990-01-01")
                            .param("aceitaContato", "true"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/doadores/" + id + "/editar"));

            final DoadorCadastrado editado = doadorRepository.buscarPorId(id).orElseThrow();
            assertThat(editado.nome()).isEqualTo("Nome Corrigido");
            assertThat(editado.tipoSanguineo()).isEqualTo(TipoSanguineo.O_NEGATIVO);
        }

        @Test
        @DisplayName("recusa e-mail de outro doador e mantem o que foi digitado")
        void recusaEmailDuplicado() throws Exception {
            final String outro = "outro-" + UUID.randomUUID() + "@example.org";
            doadorRepository.salvar(new NovoDoador("Outro", outro, null, TipoSanguineo.A_POSITIVO,
                    Sexo.FEMININO, LocalDate.of(1985, 1, 1), null, true));

            mockMvc.perform(post("/doadores/" + id + "/editar").with(csrf())
                            .param("nome", "Tentativa")
                            .param("email", outro)
                            .param("tipoSanguineo", "B_NEGATIVO")
                            .param("sexo", "MASCULINO")
                            .param("dataNascimento", "1990-01-01")
                            // O que o navegador envia quando a caixa de autorizacao fica desmarcada.
                            .param("_aceitaContato", "on"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrors("dadosDoador", "email"))
                    .andExpect(content().string(Matchers.containsString("Tentativa")));

            assertThat(doadorRepository.buscarPorId(id).orElseThrow().email()).isEqualTo(email);
        }

        @Test
        @DisplayName("desativado sai da busca, aparece entre os desativados e pode ser reativado")
        void desativaEReativa() throws Exception {
            mockMvc.perform(post("/doadores/" + id + "/desativar").with(csrf()))
                    .andExpect(status().is3xxRedirection());

            final var lista = mockMvc.perform(get("/doadores").param("busca", email)).andReturn()
                    .getModelAndView().getModel();
            @SuppressWarnings("unchecked")
            final var encontrados = (List<DoadorListado>) lista.get("doadores");
            @SuppressWarnings("unchecked")
            final var inativos = (List<DoadorCadastrado>) lista.get("inativos");
            assertThat(encontrados).isEmpty();
            assertThat(inativos).extracting(DoadorCadastrado::id).contains(id);

            mockMvc.perform(post("/doadores/" + id + "/reativar").with(csrf()))
                    .andExpect(status().is3xxRedirection());
            assertThat(doadorRepository.buscarPorId(id).orElseThrow().ativo()).isTrue();
        }

        @Test
        @DisplayName("NAO exclui: apagar nao tem volta e fica com o responsavel")
        void naoExclui() throws Exception {
            mockMvc.perform(post("/doadores/" + id + "/excluir").param("confirmacao", "true").with(csrf()))
                    .andExpect(status().isForbidden());

            assertThat(doadorRepository.buscarPorId(id)).isPresent();
        }

        @Test
        @DisplayName("exporta os dados do titular como arquivo JSON")
        void exporta() throws Exception {
            mockMvc.perform(get("/doadores/" + id + "/exportar"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", Matchers.containsString("attachment")))
                    .andExpect(jsonPath("$.cadastro.email").value(email))
                    .andExpect(jsonPath("$.cadastro.tipoSanguineo").value("B-"))
                    .andExpect(jsonPath("$.consentimentos").isArray());
        }
    }

    @Nested
    @DisplayName("como responsavel")
    @WithMockUser(username = "chefe@example.org", roles = "RESPONSAVEL")
    class ComoResponsavel {

        @Test
        @DisplayName("sem a confirmacao marcada, nao exclui")
        void exigeConfirmacao() throws Exception {
            mockMvc.perform(post("/doadores/" + id + "/excluir").with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("erro"));

            assertThat(doadorRepository.buscarPorId(id)).isPresent();
        }

        @Test
        @DisplayName("exclui o doador e, em cascata, as doacoes dele")
        void exclui() throws Exception {
            doacaoRepository.registrar(new NovaDoacao(id, LocalDate.now().minusDays(200), null, null));

            mockMvc.perform(post("/doadores/" + id + "/excluir").param("confirmacao", "true").with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/doadores"));

            assertThat(doadorRepository.buscarPorId(id)).isEmpty();
            assertThat(doacaoRepository.existeNoDia(id, LocalDate.now().minusDays(200))).isFalse();
        }
    }

    @Nested
    @DisplayName("o titular, pelo link dos e-mails e sem login")
    class Titular {

        @Test
        @DisplayName("ve a propria pagina")
        void vePagina() throws Exception {
            mockMvc.perform(get("/meus-dados/" + token()))
                    .andExpect(status().isOk())
                    .andExpect(content().string(Matchers.containsString("Doador de Teste")));
        }

        @Test
        @DisplayName("baixa os proprios dados")
        void baixa() throws Exception {
            mockMvc.perform(get("/meus-dados/" + token() + "/exportar"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cadastro.email").value(email))
                    .andExpect(jsonPath("$.tokenDescadastro").doesNotExist());
        }

        @Test
        @DisplayName("exclui os proprios dados, com confirmacao")
        void exclui() throws Exception {
            final String token = token();

            mockMvc.perform(post("/meus-dados/" + token + "/excluir").param("confirmacao", "true").with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("excluido", true));

            assertThat(doadorRepository.buscarPorId(id)).isEmpty();
        }

        @Test
        @DisplayName("link desconhecido nao mostra dado nenhum")
        void linkDesconhecido() throws Exception {
            mockMvc.perform(get("/meus-dados/nao-existe"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(Matchers.containsString("Link inválido")));
        }
    }
}
