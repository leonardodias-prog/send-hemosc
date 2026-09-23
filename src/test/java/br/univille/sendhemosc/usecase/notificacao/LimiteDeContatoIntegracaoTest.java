package br.univille.sendhemosc.usecase.notificacao;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.NotificacaoEntity;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.NotificacaoJpaRepository;
import br.univille.sendhemosc.domain.dto.NovoDoador;
import br.univille.sendhemosc.domain.enums.NivelEstoque;
import br.univille.sendhemosc.domain.enums.OrigemNotificacao;
import br.univille.sendhemosc.domain.enums.Sexo;
import br.univille.sendhemosc.domain.enums.StatusNotificacao;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * O limite de contato de ponta a ponta: da convocacao pela tela ao historico no banco, passando
 * pelas consultas nativas que contam as convocacoes de cada pessoa. Tudo em transacao desfeita ao
 * fim de cada teste.
 *
 * <p>Com o envio em modo log nada sai da maquina, mas a convocacao e registrada como enviada:
 * e exatamente o que o limite precisa enxergar na rodada seguinte.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Limite de contato, da tela ao banco")
class LimiteDeContatoIntegracaoTest {

    private static final String BUSCA = "limite.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IDoadorRepositoryPort doadorRepository;

    @Autowired
    private NotificacaoJpaRepository notificacaoRepository;

    private Long primeiro;

    @BeforeEach
    void cadastrarDoadores() {
        primeiro = doadorRepository.salvar(new NovoDoador("Limite Um", "limite.um@example.org", null,
                TipoSanguineo.O_NEGATIVO, Sexo.MASCULINO, LocalDate.of(1990, 1, 1), BigDecimal.valueOf(70), true));
        doadorRepository.salvar(new NovoDoador("Limite Dois", "limite.dois@example.org", null,
                TipoSanguineo.O_NEGATIVO, Sexo.FEMININO, LocalDate.of(1992, 5, 20), BigDecimal.valueOf(62), true));
    }

    /** Convocacao ignorada, enviada ha tantos dias: o historico que so o tempo produziria. */
    private void convocacaoSemRespostaHa(final Long doadorId, final int dias) {
        final LocalDateTime quando = LocalDateTime.now().minusDays(dias);

        notificacaoRepository.save(NotificacaoEntity.builder()
                .doadorId(doadorId)
                .tipoSanguineoAlvo(TipoSanguineo.O_NEGATIVO.getSigla())
                .nivelEstoque(NivelEstoque.CRITICO)
                .status(StatusNotificacao.ENVIADA)
                .origem(OrigemNotificacao.MANUAL)
                .tentativa(1)
                .enviadaEm(quando)
                .criadoEm(quando)
                .build());
    }

    @Nested
    @DisplayName("intervalo entre convocacoes")
    @WithMockUser(username = "chefe@example.org", roles = "RESPONSAVEL")
    class Intervalo {

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

            mockMvc.perform(get("/doadores").param("busca", BUSCA))
                    .andExpect(content().string(containsString("Convocado em")));
        }
    }

    @Nested
    @DisplayName("teto de convocacoes sem resposta")
    @WithMockUser(username = "chefe@example.org", roles = "RESPONSAVEL")
    class Teto {

        @Test
        @DisplayName("tres convocacoes ignoradas dentro do prazo seguram a pessoa, mesmo com o intervalo cumprido")
        void tetoDentroDoPrazo() throws Exception {
            convocacaoSemRespostaHa(primeiro, 120);
            convocacaoSemRespostaHa(primeiro, 80);
            convocacaoSemRespostaHa(primeiro, 40);

            mockMvc.perform(get("/doadores").param("busca", "limite.um"))
                    .andExpect(content().string(containsString("Sem resposta a")));
        }

        @Test
        @DisplayName("passado o prazo, as convocacoes ignoradas deixam de contar e a pessoa volta a ser chamada")
        void prazoDesfazOTeto() throws Exception {
            convocacaoSemRespostaHa(primeiro, 260);
            convocacaoSemRespostaHa(primeiro, 220);
            convocacaoSemRespostaHa(primeiro, 190);

            mockMvc.perform(get("/doadores").param("busca", "limite.um"))
                    .andExpect(content().string(not(containsString("Sem resposta a"))))
                    .andExpect(content().string(containsString("Pode doar")));
        }
    }

    @Nested
    @DisplayName("liberacao pelo administrador")
    class Liberacao {

        @Test
        @WithMockUser(username = "admin@example.org", roles = "MASTER")
        @DisplayName("o administrador libera quem esta no teto, e a pessoa volta a poder ser convocada na hora")
        void administradorLibera() throws Exception {
            convocacaoSemRespostaHa(primeiro, 120);
            convocacaoSemRespostaHa(primeiro, 80);
            convocacaoSemRespostaHa(primeiro, 5);

            mockMvc.perform(get("/doadores").param("busca", "limite.um"))
                    .andExpect(content().string(containsString("Liberar contato")));

            mockMvc.perform(post("/doadores/" + primeiro + "/liberar-contato").param("busca", "limite.um").with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/doadores?busca=limite.um"))
                    .andExpect(flash().attribute("aviso", containsString("Limite de contato de Limite Um liberado")));

            // Liberar zera o teto e tambem o intervalo: a convocacao de cinco dias atras deixa de contar.
            mockMvc.perform(get("/doadores").param("busca", "limite.um"))
                    .andExpect(content().string(containsString("Pode doar")))
                    .andExpect(content().string(not(containsString("Liberar contato"))));
        }

        @Test
        @WithMockUser(roles = "RESPONSAVEL")
        @DisplayName("o responsavel nao libera: desfazer o limite de alguem e decisao do administrador")
        void responsavelNao() throws Exception {
            mockMvc.perform(post("/doadores/" + primeiro + "/liberar-contato").with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "OPERADOR")
        @DisplayName("o operador tambem nao")
        void operadorNao() throws Exception {
            mockMvc.perform(post("/doadores/" + primeiro + "/liberar-contato").with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }
}
