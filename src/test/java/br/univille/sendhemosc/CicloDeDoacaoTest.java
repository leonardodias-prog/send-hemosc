package br.univille.sendhemosc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.NotificacaoEntity;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.NotificacaoJpaRepository;
import br.univille.sendhemosc.domain.dto.NovoDoador;
import br.univille.sendhemosc.domain.enums.Sexo;
import br.univille.sendhemosc.domain.enums.StatusNotificacao;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * O ciclo completo que justifica o sistema, de ponta a ponta: a pessoa apta e convocada, doa, a
 * doacao registrada fecha a convocacao, e a aptidao recalculada tira a pessoa da fila ate cumprir
 * o intervalo. Cada passo passa pela tela, e o banco e conferido no caminho.
 *
 * <p>Os testes de cada use case cobrem as regras isoladas; este cobre a costura entre eles, que e
 * onde o ciclo ja quebrou antes: a doacao era registrada, mas a convocacao seguia aberta e a pessoa
 * continuava sendo chamada.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "chefe@example.org", roles = "RESPONSAVEL")
@DisplayName("Ciclo de doacao, de ponta a ponta")
class CicloDeDoacaoTest {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IDoadorRepositoryPort doadorRepository;

    @Autowired
    private NotificacaoJpaRepository notificacaoRepository;

    @Autowired
    private EntityManager entityManager;

    private Long cadastrar(final String busca, final Sexo sexo) {
        return doadorRepository.salvar(new NovoDoador("Ciclo " + sexo, busca + "@example.org", null,
                TipoSanguineo.O_NEGATIVO, sexo, LocalDate.of(1990, 1, 1), BigDecimal.valueOf(70), true));
    }

    /** A atualizacao das convocacoes e em lote e nao passa pelo contexto de persistencia. */
    private List<NotificacaoEntity> convocacoesDe(final Long doadorId) {
        entityManager.flush();
        entityManager.clear();

        return notificacaoRepository.findAll().stream()
                .filter(n -> n.getDoadorId().equals(doadorId))
                .toList();
    }

    @Test
    @DisplayName("convocado, doa e sai da fila: a convocacao fecha e volta a poder doar em 60 dias")
    void cicloCompletoMasculino() throws Exception {
        final String busca = "ciclo.masculino";
        final Long id = cadastrar(busca, Sexo.MASCULINO);
        final LocalDate hoje = LocalDate.now();
        final String volta = hoje.plusDays(60).format(DATA);

        // 1. Nunca doou: apto e convocavel.
        mockMvc.perform(get("/doadores").param("busca", busca))
                .andExpect(content().string(containsString("Pode doar")))
                .andExpect(content().string(containsString("nunca doou")));

        // 2. Convocacao pela tela. Em modo log nada sai da maquina, mas fica registrada como enviada.
        mockMvc.perform(post("/doadores/" + id + "/convocar").param("busca", busca).with(csrf()))
                .andExpect(flash().attribute("aviso", "1 convocação(ões) enviada(s)."));

        assertThat(convocacoesDe(id))
                .singleElement()
                .satisfies(n -> assertThat(n.getStatus()).isEqualTo(StatusNotificacao.ENVIADA));

        // 3. A pessoa atende e a equipe registra a doacao.
        mockMvc.perform(post("/doadores/" + id + "/doacao").param("dataDoacao", hoje.toString())
                        .param("busca", busca).with(csrf()))
                .andExpect(flash().attribute("aviso", containsString("Doação de Ciclo MASCULINO registrada.")))
                .andExpect(flash().attribute("aviso",
                        containsString("1 convocação(ões) passaram a constar como atendidas.")))
                .andExpect(flash().attribute("aviso", not(containsString("inapta"))));

        // 4. A convocacao aberta foi fechada com a data da doacao.
        assertThat(convocacoesDe(id))
                .singleElement()
                .satisfies(n -> {
                    assertThat(n.getStatus()).isEqualTo(StatusNotificacao.COMPARECEU);
                    assertThat(n.getCompareceuEm()).isEqualTo(hoje);
                });

        // 5. A aptidao recalculada tira a pessoa da fila ate cumprir o intervalo masculino.
        mockMvc.perform(get("/doadores").param("busca", busca))
                .andExpect(content().string(not(containsString("Pode doar"))))
                .andExpect(content().string(containsString("Apto em <span>" + volta + "</span>")))
                .andExpect(content().string(containsString(hoje.format(DATA))));

        // 6. E uma nova convocacao nao a alcanca.
        mockMvc.perform(post("/doadores/" + id + "/convocar").param("busca", busca).with(csrf()))
                .andExpect(flash().attribute("erro", "Este doador não está apto ou não autorizou receber convocações."));

        assertThat(convocacoesDe(id)).hasSize(1);
    }

    @Test
    @DisplayName("o intervalo depende do sexo: 90 dias para doadora")
    void intervaloFeminino() throws Exception {
        final String busca = "ciclo.feminino";
        final Long id = cadastrar(busca, Sexo.FEMININO);
        final LocalDate hoje = LocalDate.now();

        mockMvc.perform(post("/doadores/" + id + "/doacao").param("dataDoacao", hoje.toString())
                        .param("busca", busca).with(csrf()))
                .andExpect(flash().attribute("aviso", not(containsString("atendidas"))));

        mockMvc.perform(get("/doadores").param("busca", busca))
                .andExpect(content().string(containsString("Apto em <span>" + hoje.plusDays(90).format(DATA) + "</span>")));
    }

    @Test
    @DisplayName("doacao antiga, com o intervalo ja cumprido, devolve a pessoa a fila")
    void intervaloCumpridoVoltaAFila() throws Exception {
        final String busca = "ciclo.antigo";
        final Long id = cadastrar(busca, Sexo.MASCULINO);

        mockMvc.perform(post("/doadores/" + id + "/doacao")
                        .param("dataDoacao", LocalDate.now().minusDays(61).toString())
                        .param("busca", busca).with(csrf()))
                .andExpect(flash().attribute("aviso", containsString("registrada")));

        mockMvc.perform(get("/doadores").param("busca", busca))
                .andExpect(content().string(containsString("Pode doar")));
    }
}
