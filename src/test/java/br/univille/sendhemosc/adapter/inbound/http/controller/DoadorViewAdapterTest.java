package br.univille.sendhemosc.adapter.inbound.http.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Tela de doadores")
class DoadorViewAdapterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("operador consulta a listagem")
    void operadorConsulta() throws Exception {
        mockMvc.perform(get("/doadores"))
                .andExpect(status().isOk())
                .andExpect(view().name("doadores"))
                .andExpect(model().attributeExists("doadores", "filtro", "totalConvocaveis"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("operador NAO convoca selecao: o envio produz efeito fora do sistema")
    void operadorNaoConvocaSelecao() throws Exception {
        mockMvc.perform(post("/doadores/convocar").param("selecionados", "1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("operador NAO convoca um doador especifico")
    void operadorNaoConvocaIndividual() throws Exception {
        mockMvc.perform(post("/doadores/1/convocar").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RESPONSAVEL")
    @DisplayName("responsavel convoca, e selecao vazia e recusada com aviso")
    void selecaoVaziaAvisa() throws Exception {
        mockMvc.perform(post("/doadores/convocar").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/doadores"))
                .andExpect(flash().attributeExists("erro"));
    }

    @Test
    @WithMockUser(roles = "RESPONSAVEL")
    @DisplayName("convocar doador inexistente nao quebra, apenas informa")
    void doadorInexistente() throws Exception {
        mockMvc.perform(post("/doadores/99999/convocar").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erro"));
    }

    @Test
    @WithMockUser(roles = "RESPONSAVEL")
    @DisplayName("filtro por tipo sanguineo e aceito")
    void filtroPorTipo() throws Exception {
        mockMvc.perform(get("/doadores").param("tipo", "O-").param("apenasAptos", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("doadores"));
    }

    @Test
    @DisplayName("sem autenticacao a listagem nao abre")
    void exigeLogin() throws Exception {
        mockMvc.perform(get("/doadores")).andExpect(status().is3xxRedirection());
    }
}
