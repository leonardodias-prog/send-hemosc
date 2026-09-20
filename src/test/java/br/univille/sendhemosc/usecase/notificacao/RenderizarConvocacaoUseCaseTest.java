package br.univille.sendhemosc.usecase.notificacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.univille.sendhemosc.domain.dto.CandidatoConvocacao;
import br.univille.sendhemosc.domain.dto.MensagemEmail;
import br.univille.sendhemosc.domain.dto.SituacaoEstoque;
import br.univille.sendhemosc.domain.enums.NivelEstoque;
import br.univille.sendhemosc.domain.enums.Sexo;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Renderiza o template real. Cobre a regressao do link de descadastro: expressoes @{...} do
 * Thymeleaf exigem contexto web e quebram fora de uma requisicao, entao o link precisa ser
 * montado como URL absoluta a partir de sendhemosc.notificacao.url-base.
 */
@SpringBootTest
@DisplayName("Renderizacao do e-mail de convocacao")
class RenderizarConvocacaoUseCaseTest {

    @Autowired
    private RenderizarConvocacaoUseCase useCase;

    private static final CandidatoConvocacao DOADOR = new CandidatoConvocacao(
            1L, "Maria Silva", "maria@example.org", TipoSanguineo.O_NEGATIVO, "tok-123",
            Sexo.FEMININO, LocalDate.now().minusYears(30), BigDecimal.valueOf(65), null, 0);

    private static final SituacaoEstoque SITUACAO = new SituacaoEstoque(
            TipoSanguineo.O_NEGATIVO, 11, 60, 18, NivelEstoque.CRITICO);

    @Test
    @DisplayName("o template renderiza sem contexto web")
    void renderizaForaDeRequisicao() {
        final MensagemEmail mensagem = useCase.execute(DOADOR, SITUACAO);

        assertThat(mensagem.corpoHtml()).isNotBlank();
        assertThat(mensagem.destinatario()).isEqualTo("maria@example.org");
    }

    @Test
    @DisplayName("todo e-mail carrega o link de descadastro exigido pela LGPD")
    void incluiLinkDeDescadastro() {
        final MensagemEmail mensagem = useCase.execute(DOADOR, SITUACAO);

        assertThat(mensagem.corpoHtml()).contains("http://localhost:8080/descadastro/tok-123");
    }

    @Test
    @DisplayName("o corpo traz o tipo necessario, o nivel e o percentual")
    void incluiDadosDoEstoque() {
        final MensagemEmail mensagem = useCase.execute(DOADOR, SITUACAO);

        assertThat(mensagem.corpoHtml()).contains("O-", "Critico", "18");
        assertThat(mensagem.corpoHtml()).contains("Maria Silva");
    }

    @Test
    @DisplayName("o assunto identifica o tipo sanguineo e o nivel")
    void assuntoDescritivo() {
        final MensagemEmail mensagem = useCase.execute(DOADOR, SITUACAO);

        assertThat(mensagem.assunto()).contains("O-").contains("critico");
    }

    @Test
    @DisplayName("o rodape deixa explicito que e um prototipo academico")
    void avisoDePrototipo() {
        final MensagemEmail mensagem = useCase.execute(DOADOR, SITUACAO);

        assertThat(mensagem.corpoHtml()).containsIgnoringCase("PROTÓTIPO ACADÊMICO");
    }
}
