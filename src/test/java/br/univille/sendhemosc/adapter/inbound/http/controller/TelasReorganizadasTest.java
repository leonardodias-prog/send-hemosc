package br.univille.sendhemosc.adapter.inbound.http.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cada coisa no seu lugar: o painel mostra o estoque e um unico botao de convocacao; cadastro,
 * historico e envio de e-mail tem as suas telas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Telas reorganizadas")
class TelasReorganizadasTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "RESPONSAVEL")
    @DisplayName("o painel nao tem mais aviso de envio, cadastro nem botao por tipo")
    void painelLimpo() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("planoGeral"))
                .andExpect(content().string(containsString("Convocar doadores aptos")))
                .andExpect(content().string(not(containsString("Envio de e-mail:"))))
                .andExpect(content().string(not(containsString("Cadastrar doador"))))
                .andExpect(content().string(not(containsString("Últimas movimentações"))));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("operador nao ve o botao de convocacao nem calcula o plano")
    void operadorSemConvocacao() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("planoGeral"))
                .andExpect(content().string(not(containsString("Convocar doadores aptos"))));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("operador NAO dispara a convocacao geral")
    void operadorNaoConvoca() throws Exception {
        mockMvc.perform(post("/convocar/em-falta").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "responsavel@example.org", roles = "RESPONSAVEL")
    @DisplayName("responsavel dispara a convocacao geral e volta ao painel com o resultado")
    void responsavelConvoca() throws Exception {
        mockMvc.perform(post("/convocar/em-falta").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("aviso"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("o cadastro de doador tem tela propria")
    void telaDeCadastro() throws Exception {
        mockMvc.perform(get("/doadores/novo"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cadastrar doador")))
                .andExpect(content().string(containsString("class=\"lateral\"")));
    }

    @Test
    @WithMockUser(roles = "RESPONSAVEL")
    @DisplayName("ligar ou desligar o envio volta para a tela de envio")
    void envioVoltaParaATelaDeEnvio() throws Exception {
        mockMvc.perform(post("/envio/desligar").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/disparo-automatico"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("relatorios trazem a serie, o estoque, a base por tipo e as movimentacoes")
    void relatorios() throws Exception {
        mockMvc.perform(get("/relatorios/conversao").param("dias", "90"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("evolucao", "situacoes", "basePorTipo", "movimentacoes"))
                .andExpect(content().string(containsString("Uma coluna por semana")));
        mockMvc.perform(get("/relatorios"))
                .andExpect(redirectedUrl("/relatorios/conversao"));
    }
}
