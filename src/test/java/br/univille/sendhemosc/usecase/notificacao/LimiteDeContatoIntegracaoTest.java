package br.univille.sendhemosc.usecase.notificacao;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;

import br.univille.sendhemosc.domain.dto.NovoDoador;
import br.univille.sendhemosc.domain.enums.Sexo;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * O limite de contato de ponta a ponta: da convocacao pela tela ao historico no banco, passando
 * pelas consultas nativas que contam as convocacoes de cada pessoa.
 *
 * <p>Com o envio em modo log nada sai da maquina, mas a convocacao e registrada como enviada:
 * e exatamente o que o limite precisa enxergar na rodada seguinte.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "chefe@example.org", roles = "RESPONSAVEL")
@DisplayName("Limite de contato, da tela ao banco")
class LimiteDeContatoIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IDoadorRepositoryPort doadorRepository;

    @BeforeEach
    void cadastrarDoadores() {
        doadorRepository.salvar(new NovoDoador("Limite Um", "limite.um@example.org", null,
                TipoSanguineo.O_NEGATIVO, Sexo.MASCULINO, LocalDate.of(1990, 1, 1), BigDecimal.valueOf(70), true));
        doadorRepository.salvar(new NovoDoador("Limite Dois", "limite.dois@example.org", null,
                TipoSanguineo.O_NEGATIVO, Sexo.FEMININO, LocalDate.of(1992, 5, 20), BigDecimal.valueOf(62), true));
    }

    @Test
    @DisplayName("convocar duas vezes seguidas: a segunda nao reenvia para ninguem, e diz por que")
    void segundaConvocacaoNaoReenvia() throws Exception {
        mockMvc.perform(post("/convocar/O-").with(csrf()))
                .andExpect(flash().attribute("aviso", containsString("convocado(s) para O-")));

        mockMvc.perform(post("/convocar/O-").with(csrf()))
                .andExpect(flash().attribute("aviso", containsString("Nenhum doador convocado para O-")))
                .andExpect(flash().attribute("aviso", containsString("convocados há pouco tempo")));
    }

    @Test
    @DisplayName("depois de convocada, a pessoa aparece na listagem como convocada, com a data")
    void listagemMostraAConvocacao() throws Exception {
        mockMvc.perform(post("/convocar/O-").with(csrf()));

        mockMvc.perform(get("/doadores").param("busca", "limite."))
                .andExpect(content().string(containsString("Convocado em")));
    }
}
