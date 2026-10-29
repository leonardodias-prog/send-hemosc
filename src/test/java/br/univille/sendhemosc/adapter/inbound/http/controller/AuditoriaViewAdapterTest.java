package br.univille.sendhemosc.adapter.inbound.http.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Tela de auditoria")
class AuditoriaViewAdapterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IAuditoriaPort auditoria;

    @Test
    @WithMockUser(username = "admin@example.org", roles = "MASTER")
    @DisplayName("administrador ve o que foi registrado, com o nome legivel da acao")
    void administradorVe() throws Exception {
        auditoria.registrar("ENVIO_LIGADO", "Envio ligado pelo teste de auditoria");

        mockMvc.perform(get("/auditoria"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Envio ligado pelo teste de auditoria")))
                .andExpect(content().string(containsString("Envio de e-mail ligado")))
                .andExpect(content().string(containsString("href=\"/auditoria\"")));
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "MASTER")
    @DisplayName("o filtro por acao deixa de fora as demais")
    void filtraPorAcao() throws Exception {
        auditoria.registrar("ENVIO_LIGADO", "registro que deve aparecer");
        auditoria.registrar("ENVIO_DESLIGADO", "registro que deve sumir");

        mockMvc.perform(get("/auditoria").param("acao", "ENVIO_LIGADO"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("registro que deve aparecer")))
                .andExpect(content().string(not(containsString("registro que deve sumir"))));
    }

    @Test
    @WithMockUser(roles = "RESPONSAVEL")
    @DisplayName("responsavel NAO ve a auditoria, nem o item na lateral")
    void responsavelNaoVe() throws Exception {
        mockMvc.perform(get("/auditoria"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/"))
                .andExpect(content().string(not(containsString("href=\"/auditoria\""))));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("operador NAO ve a auditoria")
    void operadorNaoVe() throws Exception {
        mockMvc.perform(get("/auditoria"))
                .andExpect(status().isForbidden());
    }
}
