package br.univille.sendhemosc.adapter.inbound.http.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.NotificacaoEntity;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.NotificacaoJpaRepository;
import br.univille.sendhemosc.domain.dto.ConversaoPorTipo;
import br.univille.sendhemosc.domain.dto.NovoDoador;
import br.univille.sendhemosc.domain.dto.RelatorioConversao;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Relatorio de conversao: quantos convocados, de um periodo escolhido a dedo, viraram doacao.
 *
 * <p>Em transacao desfeita ao fim de cada teste, para o periodo nao enxergar nada alem do que
 * o teste criou.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Relatorio de conversao")
class RelatorioViewAdapterTest {

    private static final LocalDate INICIO_PERIODO = LocalDate.of(2020, 1, 1);
    private static final LocalDate FIM_PERIODO = LocalDate.of(2020, 1, 31);
    private static final LocalDateTime ENVIADA_EM = LocalDateTime.of(2020, 1, 15, 10, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IDoadorRepositoryPort doadorRepository;

    @Autowired
    private NotificacaoJpaRepository notificacaoRepository;

    private Long doadorId;

    @BeforeEach
    void cadastrarDoador() {
        doadorId = doadorRepository.salvar(new NovoDoador("Conversao Teste", "conversao.teste@example.org", null,
                TipoSanguineo.O_NEGATIVO, Sexo.MASCULINO, LocalDate.of(1990, 1, 1), BigDecimal.valueOf(70), true));
    }

    private void registrarNotificacao(final StatusNotificacao status) {
        notificacaoRepository.save(NotificacaoEntity.builder()
                .doadorId(doadorId)
                .tipoSanguineoAlvo(TipoSanguineo.O_NEGATIVO.getSigla())
                .nivelEstoque(NivelEstoque.CRITICO)
                .status(status)
                .origem(OrigemNotificacao.MANUAL)
                .tentativa(1)
                .enviadaEm(ENVIADA_EM)
                .compareceuEm(status == StatusNotificacao.COMPARECEU ? ENVIADA_EM.toLocalDate() : null)
                .criadoEm(ENVIADA_EM)
                .build());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("conta convocados e convertidos no periodo, por tipo sanguineo")
    void contaPorTipoNoPeriodo() throws Exception {
        registrarNotificacao(StatusNotificacao.ENVIADA);
        registrarNotificacao(StatusNotificacao.COMPARECEU);

        final var resultado = mockMvc.perform(get("/relatorios/conversao")
                        .param("dataInicio", INICIO_PERIODO.toString())
                        .param("dataFim", FIM_PERIODO.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("relatorio-conversao"))
                .andReturn();

        final RelatorioConversao relatorio =
                (RelatorioConversao) resultado.getModelAndView().getModel().get("relatorio");

        assertThat(relatorio.porTipo()).hasSize(TipoSanguineo.values().length);
        final ConversaoPorTipo linhaNegativoO = relatorio.porTipo().stream()
                .filter(linha -> linha.tipoSanguineo() == TipoSanguineo.O_NEGATIVO)
                .findFirst().orElseThrow();

        assertThat(linhaNegativoO.totalConvocados()).isEqualTo(2);
        assertThat(linhaNegativoO.totalConvertidos()).isEqualTo(1);
        assertThat(linhaNegativoO.taxaConversao()).isEqualTo(50.0);
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("fora do periodo escolhido, nao conta")
    void foraDoPeriodoNaoConta() throws Exception {
        registrarNotificacao(StatusNotificacao.ENVIADA);

        final var resultado = mockMvc.perform(get("/relatorios/conversao")
                        .param("dataInicio", INICIO_PERIODO.plusMonths(1).toString())
                        .param("dataFim", FIM_PERIODO.plusMonths(1).toString()))
                .andExpect(status().isOk())
                .andReturn();

        final RelatorioConversao relatorio =
                (RelatorioConversao) resultado.getModelAndView().getModel().get("relatorio");

        assertThat(relatorio.totalConvocados()).isZero();
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("sem periodo escolhido, usa os ultimos trinta dias e abre mesmo assim")
    void semPeriodoUsaPadrao() throws Exception {
        mockMvc.perform(get("/relatorios/conversao"))
                .andExpect(status().isOk())
                .andExpect(view().name("relatorio-conversao"));
    }

    @Test
    @DisplayName("sem autenticacao, o relatorio nao abre")
    void exigeLogin() throws Exception {
        mockMvc.perform(get("/relatorios/conversao"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
