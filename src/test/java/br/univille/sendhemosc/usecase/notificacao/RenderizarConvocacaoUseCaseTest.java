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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
            1L, "Maria Silva", "maria@example.org", TipoSanguineo.O_NEGATIVO, "tok-123", true,
            Sexo.FEMININO, LocalDate.now().minusYears(30), BigDecimal.valueOf(65), null, 0, 0, null);

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

        assertThat(mensagem.corpoHtml()).contains("O-", "Crítico", "18");
        assertThat(mensagem.corpoHtml()).contains("Maria Silva");
    }

    @Test
    @DisplayName("o assunto identifica o tipo sanguineo e o nivel")
    void assuntoDescritivo() {
        final MensagemEmail mensagem = useCase.execute(DOADOR, SITUACAO);

        assertThat(mensagem.assunto()).contains("O-").contains("crítico");
    }

    @Test
    @DisplayName("o rodape deixa explicito que e um prototipo academico")
    void avisoDePrototipo() {
        final MensagemEmail mensagem = useCase.execute(DOADOR, SITUACAO);

        assertThat(mensagem.corpoHtml()).containsIgnoringCase("PROTÓTIPO ACADÊMICO");
    }

    @Test
    @DisplayName("o rodape diz que nao e comunicacao oficial do HEMOSC e quem assina a mensagem")
    void rodapeDizDeQuemE() {
        final String corpo = useCase.execute(DOADOR, SITUACAO).corpoHtml();

        assertThat(corpo).contains("não é uma comunicação oficial do HEMOSC");
        assertThat(corpo).contains("Projeto de Extensão da UNIVILLE");
    }

    @Test
    @DisplayName("o assunto tem acento e nao pressiona: sem maiusculas, sem exclamacao")
    void assuntoSemPressaoEComAcento() {
        final String assunto = useCase.execute(DOADOR, SITUACAO).assunto();

        assertThat(assunto).contains("nível", "doação");
        assertThat(assunto).isNotEqualTo(assunto.toUpperCase()).doesNotContain("!");
    }

    @Nested
    @DisplayName("o que da confianca a quem recebe")
    class Confianca {

        @Test
        @DisplayName("o logo vem por URL absoluta, que o cliente de e-mail consegue buscar fora do sistema")
        void logoPorUrlAbsoluta() {
            final String corpo = useCase.execute(DOADOR, SITUACAO).corpoHtml();

            assertThat(corpo).contains("src=\"http://localhost:8080/img/email/logo.png\"");
            assertThat(imagens(corpo)).isNotEmpty().allSatisfy(endereco -> assertThat(endereco).startsWith("http"));
        }

        @Test
        @DisplayName("o nome da marca esta em texto, de modo que continua la se o cliente bloquear imagens")
        void marcaEmTexto() {
            assertThat(useCase.execute(DOADOR, SITUACAO).corpoHtml()).contains(">Send Hemosc<");
        }

        @Test
        @DisplayName("diz por que a pessoa recebe: ela autorizou, e o tipo dela e justamente o que falta")
        void dizPorQueRecebe() {
            final String corpo = useCase.execute(DOADOR, SITUACAO).corpoHtml();

            assertThat(corpo).contains("autorizou receber convocações");
            assertThat(corpo).contains("é justamente o que está em falta");
        }

        @Test
        @DisplayName("quando o tipo da pessoa so e compativel, diz que e compativel, e o titulo fala do estoque")
        void dizQueETipoCompativel() {
            final SituacaoEstoque aPositivo = new SituacaoEstoque(
                    TipoSanguineo.A_POSITIVO, 11, 60, 18, NivelEstoque.CRITICO);
            final String corpo = useCase.execute(DOADOR, aPositivo).corpoHtml();

            assertThat(corpo).contains("é compatível com o que está em falta");
            assertThat(corpo).contains("O sangue tipo <span>A+</span> está em falta");
            assertThat(corpo).doesNotContain("é justamente o que está em falta");
        }

        @Test
        @DisplayName("quando o tipo da pessoa e o que falta, o texto fala do tipo dela, como o cartaz da campanha")
        void textoFalaDoTipoDaPessoa() {
            final String corpo = useCase.execute(DOADOR, SITUACAO).corpoHtml();

            assertThat(corpo).contains("Amigo doador,");
            assertThat(corpo).contains("O seu tipo sanguíneo, <span>O-</span>, está em falta");
            assertThat(corpo).contains("no estoque do Hemosc.");
            assertThat(corpo).contains("Precisamos de você.");
        }

        @Test
        @DisplayName("diz para onde o botao leva: a pagina de agendamento, e nao a inicial do site")
        void dizParaOndeOBotaoLeva() {
            final String corpo = useCase.execute(DOADOR, SITUACAO).corpoHtml();

            assertThat(corpo).contains("href=\"https://www.hemosc.org.br/agende-sua-doacao-area-doador.html\"");
            assertThat(corpo).contains("hemosc.org.br/agende-sua-doacao-area-doador.html</a>");
            assertThat(corpo).contains("O botão abre a página de agendamento do HEMOSC (hemosc.org.br).");
            assertThat(corpo).contains("ligue para o hemocentro mais perto");
        }

        @Test
        @DisplayName("traz o texto do cartaz: agendamento previo, divulgar a ideia, esperamos voce e o abraco da equipe")
        void textoDoCartaz() {
            final String corpo = useCase.execute(DOADOR, SITUACAO).corpoHtml();

            assertThat(corpo).contains("estamos agendando previamente as doações");
            assertThat(corpo).contains("Procure divulgar essa ideia.");
            assertThat(corpo).contains("Esperamos você.");
            assertThat(corpo).contains("#JuntosSalvamosVidas");
        }

        @Test
        @DisplayName("assina como Send Hemosc, e nao como a equipe do HEMOSC: o remetente e este sistema")
        void assinaComoSendHemosc() {
            final String corpo = useCase.execute(DOADOR, SITUACAO).corpoHtml();

            assertThat(corpo).contains("Um forte abraço de toda a equipe do Send Hemosc.");
            assertThat(corpo).doesNotContain("equipe do Hemosc.");
        }

        @Test
        @DisplayName("a ilustracao e a do tipo em falta, por URL absoluta, com a frase no alt; sem marca de orgao oficial")
        void ilustracaoDoTipoSemMarcasOficiais() {
            final String corpo = useCase.execute(DOADOR, SITUACAO).corpoHtml();

            assertThat(corpo).contains("src=\"http://localhost:8080/img/email/tipo-o-negativo.png\"");
            assertThat(corpo).contains("alt=\"Você é o tipo certo: O-\"");
            assertThat(corpo).doesNotContain("FAHECE", "Governo de Santa Catarina", "Secretaria da Saúde");
        }

        @Test
        @DisplayName("cada tipo sanguineo tem o nome de arquivo certo para a sua ilustracao")
        void arquivoDaIlustracaoPorTipo() {
            assertThat(RenderizarConvocacaoUseCase.arquivoDaIlustracao(TipoSanguineo.A_POSITIVO))
                    .isEqualTo("tipo-a-positivo.png");
            assertThat(RenderizarConvocacaoUseCase.arquivoDaIlustracao(TipoSanguineo.AB_NEGATIVO))
                    .isEqualTo("tipo-ab-negativo.png");
        }

        @Test
        @DisplayName("e-mail sem script, sem folha de estilo nem fonte de fora: so o logo vem de fora")
        void semNadaExterno() {
            final String corpo = useCase.execute(DOADOR, SITUACAO).corpoHtml().toLowerCase(Locale.ROOT);

            assertThat(corpo).doesNotContain("<script", "<link", "@import", "<iframe", "<form", "javascript:");
        }

        @Test
        @DisplayName("traz a previa da caixa de entrada, oculta no corpo")
        void trazPrevia() {
            final String corpo = useCase.execute(DOADOR, SITUACAO).corpoHtml();

            assertThat(corpo).contains("O estoque de O- está em nível crítico e o seu tipo é compatível.");
            assertThat(corpo).contains("display:none");
        }

        @Test
        @DisplayName("tem estilo para o modo escuro e para a tela de celular")
        void modoEscuroECelular() {
            final String corpo = useCase.execute(DOADOR, SITUACAO).corpoHtml();

            assertThat(corpo).contains("prefers-color-scheme: dark", "max-width: 480px", "color-scheme");
        }
    }

    @Nested
    @DisplayName("o nivel do estoque aparece com a cor do sistema")
    class CorDoNivel {

        private String corpoDo(final NivelEstoque nivel, final int percentual) {
            return useCase.execute(DOADOR, new SituacaoEstoque(TipoSanguineo.O_NEGATIVO, 10, 60, percentual, nivel))
                    .corpoHtml();
        }

        @Test
        @DisplayName("critico em vermelho, atencao em ambar, normal em verde")
        void corPorNivel() {
            assertThat(corpoDo(NivelEstoque.CRITICO, 18)).contains("background-color:#b42318");
            assertThat(corpoDo(NivelEstoque.ATENCAO, 45)).contains("background-color:#a5470a");
            assertThat(corpoDo(NivelEstoque.NORMAL, 80)).contains("background-color:#08724a");
        }

        @Test
        @DisplayName("a barra reflete o percentual: o preenchido e o que falta somam 100")
        void barraSomaCem() {
            final String corpo = corpoDo(NivelEstoque.CRITICO, 18);

            assertThat(corpo).contains("width=\"18%\"", "width=\"82%\"");
        }

        @Test
        @DisplayName("estoque vazio nao desenha trecho preenchido")
        void estoqueVazio() {
            final String corpo = corpoDo(NivelEstoque.CRITICO, 0);

            assertThat(corpo).doesNotContain("width=\"0%\"").contains("width=\"100%\"");
        }

        @Test
        @DisplayName("estoque acima da capacidade alvo nao estoura a barra nem gera largura negativa")
        void estoqueAcimaDoAlvo() {
            final String corpo = corpoDo(NivelEstoque.NORMAL, 140);

            assertThat(corpo).contains("width=\"100%\"").doesNotContain("width=\"-").doesNotContain("width=\"0%\"");
        }
    }

    private static List<String> imagens(final String html) {
        final Matcher achados = Pattern.compile("<img[^>]*\\ssrc=\"([^\"]+)\"").matcher(html);
        final List<String> enderecos = new ArrayList<>();

        while (achados.find()) {
            enderecos.add(achados.group(1));
        }

        return enderecos;
    }
}
