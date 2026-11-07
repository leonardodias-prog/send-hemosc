package br.univille.sendhemosc.usecase.notificacao;

import br.univille.sendhemosc.config.SendHemoscProperties;
import br.univille.sendhemosc.domain.dto.CandidatoConvocacao;
import br.univille.sendhemosc.domain.dto.MensagemEmail;
import br.univille.sendhemosc.domain.dto.SituacaoEstoque;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Renderiza o e-mail de convocacao a partir do template Thymeleaf, incluindo sempre o link
 * de descadastro exigido pela LGPD.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RenderizarConvocacaoUseCase {

    private static final String TEMPLATE_CONVOCACAO = "email/convocacao";

    private final TemplateEngine templateEngine;
    private final SendHemoscProperties properties;

    /**
     * Monta a mensagem para um doador.
     *
     * @param doador destinatario da convocacao
     * @param situacao situacao do estoque que motivou o disparo
     * @return mensagem pronta para envio
     */
    public MensagemEmail execute(final CandidatoConvocacao doador, final SituacaoEstoque situacao) {
        final Context contexto = new Context();
        contexto.setVariable("nomeDoador", doador.nome());
        contexto.setVariable("tipoDoador", doador.tipoSanguineo().getSigla());
        contexto.setVariable("tipoNecessario", situacao.tipoSanguineo().getSigla());
        contexto.setVariable("nivel", situacao.nivel().getDescricao());
        // Chave estavel do nivel, para o modelo escolher a cor sem depender do texto exibido.
        contexto.setVariable("nivelChave", situacao.nivel().name().toLowerCase(Locale.ROOT));
        contexto.setVariable("percentual", situacao.percentualOcupacao());
        contexto.setVariable("logoUrl", properties.notificacao().urlDoLogo());
        contexto.setVariable("linkDescadastro",
                properties.notificacao().urlBase() + "/descadastro/" + doador.tokenDescadastro());
        contexto.setVariable("linkMeusDados",
                properties.notificacao().urlBase() + "/meus-dados/" + doador.tokenDescadastro());

        final String corpo = templateEngine.process(TEMPLATE_CONVOCACAO, contexto);
        final String assunto = "Estoque de sangue %s em nível %s: sua doação pode ajudar"
                .formatted(situacao.tipoSanguineo().getSigla(), situacao.nivel().getDescricao().toLowerCase(Locale.ROOT));

        return new MensagemEmail(doador.email(), assunto, corpo);
    }
}
