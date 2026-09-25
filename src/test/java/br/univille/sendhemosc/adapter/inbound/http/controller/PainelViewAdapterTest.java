package br.univille.sendhemosc.adapter.inbound.http.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import br.univille.sendhemosc.domain.dto.MovimentacaoEstoque;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Painel de captacao")
class PainelViewAdapterTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("sem autenticacao")
    class SemAutenticacao {

        @Test
        @DisplayName("o painel nao e acessivel e leva para a tela de entrada")
        void painelExigeLogin() throws Exception {
            mockMvc.perform(get("/"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        }

        @Test
        @DisplayName("a tela de entrada e publica")
        void loginEPublico() throws Exception {
            mockMvc.perform(get("/login")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("o termo de uso e publico, para ser lido antes do cadastro")
        void termoEPublico() throws Exception {
            mockMvc.perform(get("/termo"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(Matchers.containsString("sensível")));
        }
    }

    @Nested
    @DisplayName("com perfil de operador")
    @WithMockUser(username = "operador@example.org", roles = "OPERADOR")
    class ComoOperador {

        @Test
        @DisplayName("enxerga o painel")
        void enxergaOPainel() throws Exception {
            mockMvc.perform(get("/"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("painel"))
                    .andExpect(model().attributeExists("situacoes", "totalEmFalta"));
        }

        @Test
        @DisplayName("atualiza o estoque")
        void atualizaEstoque() throws Exception {
            mockMvc.perform(post("/estoque/O-").param("quantidadeBolsas", "5").with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/"))
                    .andExpect(flash().attributeExists("aviso"));
        }

        @Test
        @DisplayName("edita a capacidade alvo e a alteracao aparece no historico com o autor")
        void editaCapacidadeEFicaNoHistorico() throws Exception {
            mockMvc.perform(post("/estoque/AB-").param("quantidadeBolsas", "7").param("capacidadeAlvo", "25")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("aviso", "Estoque de AB- atualizado."));

            final var painel = mockMvc.perform(get("/"))
                    .andExpect(content().string(Matchers.containsString("Últimas movimentações")))
                    .andReturn();

            @SuppressWarnings("unchecked")
            final var movimentacoes = (List<MovimentacaoEstoque>) painel.getModelAndView().getModel().get("movimentacoes");

            assertThat(movimentacoes).anySatisfy(movimentacao -> {
                assertThat(movimentacao.tipoSanguineo()).isEqualTo(TipoSanguineo.AB_NEGATIVO);
                assertThat(movimentacao.quantidadeNova()).isEqualTo(7);
                assertThat(movimentacao.capacidadeNova()).isEqualTo(25);
                assertThat(movimentacao.responsavel()).isEqualTo("operador@example.org");
            });
        }

        @Test
        @DisplayName("recusa capacidade alvo zero e explica o motivo")
        void recusaCapacidadeZero() throws Exception {
            mockMvc.perform(post("/estoque/A-").param("quantidadeBolsas", "10").param("capacidadeAlvo", "0")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("erro", "A capacidade alvo deve ser maior que zero."));
        }

        @Test
        @DisplayName("NAO dispara convocacao: o envio produz efeito fora do sistema")
        void naoConvoca() throws Exception {
            mockMvc.perform(post("/convocar/O-").with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("NAO acessa o gerenciamento de contas")
        void naoGerenciaContas() throws Exception {
            mockMvc.perform(get("/usuarios")).andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("com perfil de responsavel")
    @WithMockUser(username = "chefe@example.org", roles = "RESPONSAVEL")
    class ComoResponsavel {

        @Test
        @DisplayName("dispara convocacao")
        void convoca() throws Exception {
            mockMvc.perform(post("/convocar/O-").with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/"))
                    .andExpect(flash().attributeExists("aviso"));
        }

        @Test
        @DisplayName("cadastra doador, como o operador")
        void cadastraDoador() throws Exception {
            mockMvc.perform(post("/estoque/B-").param("quantidadeBolsas", "0").with(csrf()))
                    .andExpect(status().is3xxRedirection());
        }

        @Test
        @DisplayName("NAO acessa o gerenciamento de contas, que e do administrador")
        void naoGerenciaContas() throws Exception {
            mockMvc.perform(get("/usuarios")).andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("com perfil de administrador")
    @WithMockUser(username = "admin@example.org", roles = "MASTER")
    class ComoAdministrador {

        @Test
        @DisplayName("acessa o gerenciamento de contas")
        void gerenciaContas() throws Exception {
            mockMvc.perform(get("/usuarios"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("usuarios"))
                    .andExpect(model().attributeExists("usuarios", "pendentes"));
        }

        @Test
        @DisplayName("tambem dispara convocacao")
        void convoca() throws Exception {
            mockMvc.perform(post("/convocar/O-").with(csrf()))
                    .andExpect(status().is3xxRedirection());
        }
    }

    @Nested
    @DisplayName("protecao contra requisicao forjada")
    @WithMockUser(username = "chefe@example.org", roles = "RESPONSAVEL")
    class Csrf {

        @Test
        @DisplayName("acao de formulario sem token e recusada")
        void semTokenERecusado() throws Exception {
            mockMvc.perform(post("/convocar/O-")).andExpect(status().isForbidden());
        }
    }
}
