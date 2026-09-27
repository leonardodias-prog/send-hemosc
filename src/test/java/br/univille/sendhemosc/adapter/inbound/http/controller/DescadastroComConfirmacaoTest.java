package br.univille.sendhemosc.adapter.inbound.http.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.univille.sendhemosc.adapter.outbound.persistence.repository.DoadorJpaRepository;
import br.univille.sendhemosc.domain.dto.NovoDoador;
import br.univille.sendhemosc.domain.enums.Sexo;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * O link de cancelamento dos e-mails em dois passos: abrir pergunta, o botao cancela. Filtro de
 * e-mail que abre o link sozinho nao descadastra ninguem.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Descadastro com confirmacao")
class DescadastroComConfirmacaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IDoadorRepositoryPort doadorRepository;

    @Autowired
    private DoadorJpaRepository doadorJpaRepository;

    @Autowired
    private EntityManager entityManager;

    private Long id;
    private String token;

    @BeforeEach
    void cadastrarDoador() {
        id = doadorRepository.salvar(new NovoDoador("Doador do Link", "doador.link@example.org", null,
                TipoSanguineo.O_NEGATIVO, Sexo.MASCULINO, LocalDate.of(1990, 1, 1), BigDecimal.valueOf(70), true));
        token = doadorJpaRepository.findById(id).orElseThrow().getTokenDescadastro();
    }

    private boolean aceitaContato() {
        return doadorRepository.buscarPorId(id).orElseThrow().aceitaContato();
    }

    /**
     * O cancelamento e uma atualizacao em lote, que nao passa pelo contexto do JPA. Fora de
     * teste cada requisicao tem transacao propria; aqui o teste inteiro e uma so, e o doador
     * lido antes continuaria em memoria com o valor antigo.
     */
    private void esquecerOQueFoiLido() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("abrir o link pergunta, e o doador continua recebendo ate clicar")
    void abrirNaoCancela() throws Exception {
        mockMvc.perform(get("/descadastro/" + token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Parar de receber convocações?")))
                .andExpect(content().string(containsString("Cancelar as convocações")));

        assertThat(aceitaContato()).isTrue();
    }

    @Test
    @DisplayName("o botao cancela na hora, e a pagina confirma")
    void botaoCancela() throws Exception {
        mockMvc.perform(post("/descadastro/" + token).with(csrf()))
                .andExpect(redirectedUrl("/descadastro/" + token))
                .andExpect(flash().attribute("efetivado", true));
        esquecerOQueFoiLido();

        assertThat(aceitaContato()).isFalse();

        mockMvc.perform(get("/descadastro/" + token).flashAttr("efetivado", true))
                .andExpect(content().string(containsString("Convocações canceladas")))
                .andExpect(content().string(not(containsString("Cancelar as convocações"))));
    }

    @Test
    @DisplayName("abrir de novo depois de cancelado diz que nada mais precisa ser feito")
    void jaCancelado() throws Exception {
        mockMvc.perform(post("/descadastro/" + token).with(csrf()));
        esquecerOQueFoiLido();

        mockMvc.perform(get("/descadastro/" + token))
                .andExpect(content().string(containsString("Convocações já canceladas")))
                .andExpect(content().string(containsString("seus dados")));
    }

    @Test
    @DisplayName("sem o token de protecao do formulario, o cancelamento nao vale")
    void semCsrf() throws Exception {
        mockMvc.perform(post("/descadastro/" + token)).andExpect(status().isForbidden());

        assertThat(aceitaContato()).isTrue();
    }

    @Test
    @DisplayName("link que nao existe diz que e invalido e nao oferece o botao")
    void linkInvalido() throws Exception {
        mockMvc.perform(get("/descadastro/nao-existe"))
                .andExpect(content().string(containsString("Link inválido")))
                .andExpect(content().string(not(containsString("Cancelar as convocações"))))
                .andExpect(content().string(not(containsString("/meus-dados/"))));
    }
}
