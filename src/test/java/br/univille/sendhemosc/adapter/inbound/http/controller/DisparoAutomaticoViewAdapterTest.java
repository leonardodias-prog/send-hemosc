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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tela do disparo automatico. As alteracoes rodam em transacao desfeita ao fim de cada teste:
 * o disparo nao pode sair daqui ligado para os demais testes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Tela do disparo automatico")
class DisparoAutomaticoViewAdapterTest {

    @Autowired
    private MockMvc mockMvc;

    private MockHttpServletRequestBuilder salvar(final String ativo, final String inicio, final String fim) {
        final MockHttpServletRequestBuilder requisicao = post("/disparo-automatico")
                .param("tipos", "O-", "A-")
                .param("limitePorRodada", "20")
                .param("horaInicio", inicio)
                .param("horaFim", fim)
                .param("intervaloDias", "1")
                .with(csrf());

        return ativo == null ? requisicao : requisicao.param("ativo", ativo);
    }

    @Nested
    @DisplayName("acesso")
    class Acesso {

        @Test
        @DisplayName("sem autenticacao leva para a tela de entrada")
        void anonimo() throws Exception {
            mockMvc.perform(get("/disparo-automatico"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        }

        @Test
        @WithMockUser(roles = "OPERADOR")
        @DisplayName("operador nao abre nem altera: o disparo manda e-mail para fora do sistema")
        void operadorNao() throws Exception {
            mockMvc.perform(get("/disparo-automatico")).andExpect(status().isForbidden());
            mockMvc.perform(salvar("true", "8", "18")).andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "OPERADOR")
        @DisplayName("operador nao ve o item na lateral")
        void operadorNaoVeOItem() throws Exception {
            mockMvc.perform(get("/"))
                    .andExpect(content().string(not(containsString("/disparo-automatico"))));
        }
    }

    @Nested
    @DisplayName("com perfil de responsavel")
    @WithMockUser(username = "chefe@example.org", roles = "RESPONSAVEL")
    class ComoResponsavel {

        @Test
        @DisplayName("abre a tela com a configuracao, a previa e a casca")
        void abre() throws Exception {
            mockMvc.perform(get("/disparo-automatico"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("disparo-automatico"))
                    .andExpect(model().attributeExists("configuracao", "ajuste", "plano"))
                    .andExpect(content().string(containsString("class=\"lateral\"")))
                    .andExpect(content().string(containsString("desligado")));
        }

        @Test
        @DisplayName("ligar grava, avisa, e passa a mostrar o indicador na lateral")
        void ligar() throws Exception {
            mockMvc.perform(salvar("true", "8", "18"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/disparo-automatico"))
                    .andExpect(flash().attribute("aviso", containsString("ligado")));

            mockMvc.perform(get("/disparo-automatico"))
                    .andExpect(content().string(containsString("As rodadas automáticas de convocação estão ligadas")))
                    .andExpect(content().string(containsString("chefe@example.org")));
        }

        @Test
        @DisplayName("janela invalida e recusada, e o formulario volta com o que foi digitado")
        void janelaInvalida() throws Exception {
            mockMvc.perform(salvar("true", "18", "8"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("erro", "ajuste"));
        }

        @Test
        @DisplayName("desmarcar a caixa desliga: checkbox desmarcado nao envia o campo")
        void desmarcarDesliga() throws Exception {
            mockMvc.perform(salvar("true", "8", "18"));

            mockMvc.perform(salvar(null, "8", "18"))
                    .andExpect(flash().attribute("aviso", containsString("desligado")));
        }
    }
}
