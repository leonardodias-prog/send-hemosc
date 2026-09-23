package br.univille.sendhemosc.adapter.inbound.http.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Protege o mapa de navegacao. Sao tres defeitos que ja existiram e nao podem voltar: tela
 * sem casca, "voltar" para um destino fixo em vez do lugar de onde a pessoa veio, e acao que
 * descarta o filtro em uso.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Casca de navegacao")
class CascaDeNavegacaoTest {

    private static final String MARCA_DA_CASCA = "class=\"lateral\"";

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("a lateral aparece em toda tela autenticada")
    @WithMockUser(username = "admin@example.org", roles = "MASTER")
    class TelasAutenticadas {

        @Test
        @DisplayName("painel")
        void painel() throws Exception {
            mockMvc.perform(get("/"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(MARCA_DA_CASCA)));
        }

        @Test
        @DisplayName("doadores")
        void doadores() throws Exception {
            mockMvc.perform(get("/doadores"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(MARCA_DA_CASCA)));
        }

        @Test
        @DisplayName("contas")
        void contas() throws Exception {
            mockMvc.perform(get("/usuarios"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(MARCA_DA_CASCA)));
        }

        @Test
        @DisplayName("termo de uso: era a tela que nao tinha navegacao nenhuma")
        void termo() throws Exception {
            mockMvc.perform(get("/termo"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(MARCA_DA_CASCA)));
        }

        @Test
        @DisplayName("o termo NAO oferece voltar para o login a quem ja entrou")
        void termoNaoMandaAutenticadoParaOLogin() throws Exception {
            mockMvc.perform(get("/termo"))
                    .andExpect(content().string(not(containsString("Voltar para a tela de entrada"))));
        }
    }

    @Nested
    @DisplayName("telas publicas")
    class TelasPublicas {

        @Test
        @DisplayName("o termo nao mostra a lateral a quem nao entrou: nao teria onde clicar")
        void termoAnonimoNaoTemCasca() throws Exception {
            mockMvc.perform(get("/termo"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString(MARCA_DA_CASCA))))
                    .andExpect(content().string(containsString("Voltar para a tela de entrada")));
        }

        @Test
        @DisplayName("o cancelamento do doador e uma pagina de verdade, com o que fazer em seguida")
        void descadastroTemSaida() throws Exception {
            mockMvc.perform(get("/descadastro/token-que-nao-existe"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("descadastro"))
                    .andExpect(content().string(containsString("sendhemosc.css")))
                    .andExpect(content().string(containsString("termo de uso")));
        }

        @Test
        @DisplayName("o link de aprovacao termina numa pagina com proximo passo")
        void aprovacaoTemProximoPasso() throws Exception {
            mockMvc.perform(get("/aprovacao/token-que-nao-existe/aprovar"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("aprovacao"))
                    .andExpect(content().string(containsString("gerenciamento de contas")));
        }

        @Test
        @DisplayName("decisao que nao existe nao vira pagina")
        void decisaoDesconhecidaNaoAbre() throws Exception {
            mockMvc.perform(get("/aprovacao/qualquer-token/talvez"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("o cadastro de doador devolve ao formulario, nao ao topo")
    @WithMockUser(username = "operador@example.org", roles = "OPERADOR")
    class CadastroNoFimDoPainel {

        @Test
        @DisplayName("cadastro aceito volta para a ancora do formulario, com a confirmacao")
        void cadastroAceito() throws Exception {
            mockMvc.perform(post("/doadores")
                            .param("nome", "Norberto Simoes")
                            .param("email", "norberto.simoes@example.org")
                            .param("tipoSanguineo", "O_NEGATIVO")
                            .param("sexo", "MASCULINO")
                            .param("dataNascimento", "1990-03-14")
                            .param("aceitaContato", "true")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/#cadastro"))
                    .andExpect(flash().attributeExists("avisoCadastro"));
        }

        @Test
        @DisplayName("cadastro recusado tambem volta ao formulario, sem obrigar a redigitar")
        void cadastroRecusado() throws Exception {
            mockMvc.perform(post("/doadores")
                            .param("nome", "")
                            .param("email", "nao-e-email")
                            .param("aceitaContato", "true")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/#cadastro"))
                    .andExpect(flash().attributeExists("novoDoador"));
        }
    }

    @Nested
    @DisplayName("a acao devolve a lista como ela estava")
    @WithMockUser(username = "chefe@example.org", roles = "RESPONSAVEL")
    class FiltroPreservado {

        @Test
        @DisplayName("convocar com filtro aplicado volta para o mesmo recorte")
        void convocarMantemOFiltro() throws Exception {
            mockMvc.perform(post("/doadores/convocar")
                            .param("selecionados", "1")
                            .param("tipo", "O-")
                            .param("apenasAptos", "true")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/doadores?tipo=O-&apenasAptos=true"));
        }

        @Test
        @DisplayName("selecao vazia tambem volta para o recorte, com o aviso")
        void selecaoVaziaMantemOFiltro() throws Exception {
            mockMvc.perform(post("/doadores/convocar").param("busca", "Beatriz").with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/doadores?busca=Beatriz"));
        }

        @Test
        @DisplayName("registrar doacao pela lista filtrada nao descarta o filtro")
        void doacaoMantemOFiltro() throws Exception {
            mockMvc.perform(post("/doadores/99999/doacao")
                            .param("dataDoacao", "2026-09-22")
                            .param("tipo", "A-")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/doadores?tipo=A-"));
        }

        @Test
        @DisplayName("sem filtro algum o destino continua limpo, sem cauda de parametros")
        void semFiltroDestinoLimpo() throws Exception {
            mockMvc.perform(post("/doadores/convocar").with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/doadores"));
        }
    }
}
