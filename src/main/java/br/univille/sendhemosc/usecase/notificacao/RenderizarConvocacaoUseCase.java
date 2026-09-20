package br.univille.sendhemosc.usecase.notificacao;

import br.univille.sendhemosc.config.SendHemoscProperties;
import br.univille.sendhemosc.domain.dto.CandidatoConvocacao;
import br.univille.sendhemosc.domain.dto.MensagemEmail;
import br.univille.sendhemosc.domain.dto.SituacaoEstoque;
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
        contexto.setVariable("percentual", situacao.percentualOcupacao());
        contexto.setVariable("linkDescadastro",
                properties.notificacao().urlBase() + "/descadastro/" + doador.tokenDescadastro());

        final String corpo = templateEngine.process(TEMPLATE_CONVOCACAO, contexto);
        final String assunto = "Estoque de sangue %s em nivel %s - sua doacao faz diferenca"
                .formatted(situacao.tipoSanguineo().getSigla(), situacao.nivel().getDescricao().toLowerCase());

        return new MensagemEmail(doador.email(), assunto, corpo);
    }
}
