package br.univille.sendhemosc.adapter.inbound.http.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Painel de captacao")
class PainelViewAdapterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("a raiz responde a pagina do painel com os oito tipos")
    void raizRenderizaPainel() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("painel"))
                .andExpect(model().attributeExists("situacoes", "totalEmFalta"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Send Hemosc")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("O-")));
    }

    @Test
    @DisplayName("o painel deixa explicito que e prototipo com dados ficticios")
    void painelAvisaQueEPrototipo() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Protótipo acadêmico")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("fictícios")));
    }

    @Test
    @DisplayName("atualizar o estoque redireciona de volta com aviso")
    void atualizaEstoque() throws Exception {
        mockMvc.perform(post("/estoque/O-").param("quantidadeBolsas", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("aviso"));
    }

    @Test
    @DisplayName("quantidade negativa e normalizada para zero em vez de quebrar")
    void quantidadeNegativaNaoQuebra() throws Exception {
        mockMvc.perform(post("/estoque/B-").param("quantidadeBolsas", "-10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("convocar redireciona de volta com o resultado")
    void convocaDoadores() throws Exception {
        mockMvc.perform(post("/convocar/O-"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("aviso"));
    }

    @Test
    @DisplayName("sigla invalida retorna 400 em vez de erro interno")
    void siglaInvalida() throws Exception {
        mockMvc.perform(post("/convocar/Z%2B"))
                .andExpect(status().isBadRequest());
    }
}
