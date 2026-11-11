package br.univille.sendhemosc.adapter.inbound.http.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import br.univille.sendhemosc.domain.dto.MensagemEmail;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.port.outbound.IAvisoEstoqueRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.IEmailPort;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Cobre a tela com o banco e os templates reais: quem entra, quem recebe e o e-mail que sai do
 * botao de teste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Tela do aviso de estoque")
class AvisoEstoqueViewAdapterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IAvisoEstoqueRepositoryPort avisoRepository;

    @Autowired
    private IUsuarioRepositoryPort usuarioRepository;

    @MockitoSpyBean
    private IEmailPort emailPort;

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("o operador nao configura o aviso")
    void operadorNaoAcessa() throws Exception {
        mockMvc.perform(get("/aviso-estoque")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "responsavel@example.org", roles = "RESPONSAVEL")
    @DisplayName("a tela mostra os administradores como unicos destinatarios e nao oferece interruptor")
    void telaMostraOsAdministradores() throws Exception {
        final var administradores = usuarioRepository.emailsDosAdministradores();
        assertThat(administradores).isNotEmpty();

        mockMvc.perform(get("/aviso-estoque"))
                .andExpect(status().isOk())
                .andExpect(view().name("aviso-estoque"))
                .andExpect(content().string(Matchers.containsString(administradores.getFirst())))
                .andExpect(content().string(Matchers.containsString("Sempre ligado")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("class=\"interruptor\""))));

        assertThat(avisoRepository.buscar().horaRelatorio()).isEqualTo(7);
    }

    @Test
    @WithMockUser(username = "responsavel@example.org", roles = "RESPONSAVEL")
    @DisplayName("nao existe mais como incluir outro endereco na lista")
    void naoHaComoIncluirDestinatario() throws Exception {
        mockMvc.perform(post("/aviso-estoque/destinatarios").param("email", "fora@example.org").with(csrf()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(username = "responsavel@example.org", roles = "RESPONSAVEL")
    @DisplayName("o teste vai so para administradores, com os oito tipos, a situacao escrita e os tipos com atencao")
    void testeVaiSoParaAdministradores() throws Exception {
        mockMvc.perform(post("/aviso-estoque/teste").with(csrf()))
                .andExpect(redirectedUrl("/aviso-estoque"))
                .andExpect(flash().attributeExists("aviso"));

        final ArgumentCaptor<MensagemEmail> mensagens = ArgumentCaptor.forClass(MensagemEmail.class);
        verify(emailPort, atLeastOnce()).enviar(mensagens.capture());

        final var doAviso = mensagens.getAllValues().stream()
                .filter(mensagem -> mensagem.assunto().startsWith("[Teste] Estoque de sangue – "))
                .toList();

        assertThat(doAviso).isNotEmpty();
        assertThat(doAviso).extracting(MensagemEmail::destinatario)
                .containsExactlyInAnyOrderElementsOf(usuarioRepository.emailsDosAdministradores());
        assertThat(doAviso.getFirst().corpoHtml())
                .contains("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
                .contains("Tipos com atenção:", "Crítico", "Normal", "http://localhost:8080/");
    }

    @Test
    @WithMockUser(username = "responsavel@example.org", roles = "RESPONSAVEL")
    @DisplayName("minimo critico maior que o baixo e recusado, e nenhum tipo e alterado")
    void recusaMinimosInvertidos() throws Exception {
        var requisicao = post("/aviso-estoque/minimos").with(csrf());

        for (final TipoSanguineo tipo : TipoSanguineo.values()) {
            requisicao = requisicao.param("critico_" + tipo.name(), "12").param("baixo_" + tipo.name(), "10");
        }

        mockMvc.perform(requisicao).andExpect(flash().attributeExists("erro"));
    }
}
