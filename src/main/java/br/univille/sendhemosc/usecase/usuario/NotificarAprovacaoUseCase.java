package br.univille.sendhemosc.usecase.usuario;

import br.univille.sendhemosc.config.SendHemoscProperties;
import br.univille.sendhemosc.domain.dto.MensagemEmail;
import br.univille.sendhemosc.domain.enums.PerfilUsuario;
import br.univille.sendhemosc.domain.port.outbound.IEmailPort;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Avisa os administradores de que ha um cadastro de responsavel aguardando decisao, com links
 * diretos para aprovar ou recusar.
 *
 * <p>Falha no aviso nao derruba o cadastro: a conta ja foi criada como pendente e continua
 * visivel na tela de gerenciamento. Perder o e-mail atrasa a aprovacao; perder o cadastro
 * obrigaria a pessoa a se inscrever de novo.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificarAprovacaoUseCase {

    private static final String TEMPLATE = "email/aprovacao-usuario";

    private final IUsuarioRepositoryPort usuarioRepository;
    private final IEmailPort emailPort;
    private final TemplateEngine templateEngine;
    private final SendHemoscProperties properties;

    /**
     * Envia o pedido de aprovacao aos administradores ativos.
     *
     * @param nome nome de quem se cadastrou
     * @param email e-mail de quem se cadastrou
     * @param perfil perfil solicitado
     * @param token token do link de decisao
     */
    public void execute(final String nome, final String email, final PerfilUsuario perfil, final String token) {
        final List<String> administradores = usuarioRepository.emailsDosAdministradores();

        if (administradores.isEmpty()) {
            log.error("[m=execute] Cadastro de {} aguarda aprovacao, mas nao ha administrador ativo "
                    + "para avisar. A conta continua pendente na tela de gerenciamento", email);
            return;
        }

        final String corpo = renderizar(nome, email, perfil, token);
        final String assunto = "Cadastro aguardando aprovacao: %s".formatted(nome);

        administradores.forEach(destinatario -> {
            try {
                emailPort.enviar(new MensagemEmail(destinatario, assunto, corpo));
            } catch (final RuntimeException excecao) {
                log.error("[m=execute] Falha ao avisar o administrador {}: {}. A conta segue pendente",
                        destinatario, excecao.getMessage());
            }
        });
    }

    private String renderizar(final String nome, final String email,
                              final PerfilUsuario perfil, final String token) {
        final String urlBase = properties.notificacao().urlBase();
        final Context contexto = new Context();

        contexto.setVariable("nomeSolicitante", nome);
        contexto.setVariable("emailSolicitante", email);
        contexto.setVariable("perfil", perfil.getDescricao());
        contexto.setVariable("atribuicoes", perfil.getAtribuicoes());
        contexto.setVariable("linkAprovar", urlBase + "/aprovacao/" + token + "/aprovar");
        contexto.setVariable("linkRecusar", urlBase + "/aprovacao/" + token + "/recusar");

        return templateEngine.process(TEMPLATE, contexto);
    }
}
