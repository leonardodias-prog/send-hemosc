package br.univille.sendhemosc.adapter.inbound.http.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Cabecalhos de seguranca e mensagens de erro")
class CabecalhosDeSegurancaTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("toda pagina sai com politica de conteudo, sem quadros e sem Referer")
    void paginaSaiComOsCabecalhos() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'self'")))
                .andExpect(header().string("Content-Security-Policy", containsString("form-action 'self'")))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Permissions-Policy", containsString("camera=()")));
    }

    @Test
    @DisplayName("o script das telas e servido sem login")
    void scriptDasTelasEPublico() throws Exception {
        mockMvc.perform(get("/js/sendhemosc.js"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RESPONSAVEL")
    @DisplayName("a API nao aceita escrita: sem token CSRF o pedido e recusado antes de chegar a rota")
    void apiExigeTokenParaEscrita() throws Exception {
        mockMvc.perform(post("/api/convocacoes/O-"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/doadores").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RESPONSAVEL")
    @DisplayName("as rotas de escrita da API sem uso nao existem mais")
    void rotasDeEscritaRemovidas() throws Exception {
        mockMvc.perform(post("/api/convocacoes/O-").with(csrf()))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(put("/api/estoque/O-").param("quantidadeBolsas", "1").with(csrf()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("a leitura do estoque pela API continua disponivel")
    void leituraDaApiContinua() throws Exception {
        mockMvc.perform(get("/api/estoque"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RESPONSAVEL")
    @DisplayName("argumento invalido devolve mensagem generica, sem o texto interno da excecao")
    void argumentoInvalidoNaoVazaTextoInterno() throws Exception {
        mockMvc.perform(post("/estoque/XYZ").param("quantidadeBolsas", "1").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQ-400"))
                .andExpect(jsonPath("$.mensagem").value("Requisicao invalida."))
                .andExpect(content().string(not(containsString("XYZ"))));
    }
}
