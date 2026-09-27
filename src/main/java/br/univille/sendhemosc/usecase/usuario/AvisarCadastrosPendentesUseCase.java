package br.univille.sendhemosc.usecase.usuario;

import br.univille.sendhemosc.config.SegurancaProperties;
import br.univille.sendhemosc.config.SendHemoscProperties;
import br.univille.sendhemosc.domain.dto.CadastroPendente;
import br.univille.sendhemosc.domain.dto.MensagemEmail;
import br.univille.sendhemosc.domain.port.outbound.IEmailPort;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Avisa os administradores de que ha cadastros de responsavel aguardando decisao.
 *
 * <p>O aviso sai no maximo uma vez por periodo, com todos os pendentes ainda nao avisados.
 * Antes saia um e-mail por cadastro para cada administrador: um script que fizesse algumas
 * centenas de cadastros esgotaria a cota diaria do provedor, e naquele dia nenhuma convocacao
 * sairia. Agrupado, o volume de e-mail deixa de acompanhar o de cadastros.</p>
 *
 * <p>O primeiro cadastro depois de um periodo quieto e avisado na hora. Os que chegam dentro do
 * periodo esperam o aviso seguinte, que o agendador manda assim que o periodo termina; enquanto
 * isso, a lateral do administrador ja mostra cada um.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AvisarCadastrosPendentesUseCase {

    private static final String TEMPLATE = "email/cadastros-pendentes";

    private final IUsuarioRepositoryPort usuarioRepository;
    private final IEmailPort emailPort;
    private final TemplateEngine templateEngine;
    private final SendHemoscProperties properties;
    private final SegurancaProperties seguranca;

    /**
     * Manda o aviso se o periodo desde o ultimo ja passou e ha cadastro novo para avisar.
     *
     * @param agora momento da verificacao
     * @return quantos cadastros entraram no aviso; zero quando nao era hora ou nao havia nada
     */
    public int executarSeDevido(final LocalDateTime agora) {
        final SegurancaProperties.Aprovacao regras = seguranca.aprovacao();
        final boolean cedoDemais = usuarioRepository.ultimoAvisoDeCadastros()
                .filter(ultimo -> ultimo.isAfter(agora.minusMinutes(regras.avisoIntervaloMinutos())))
                .isPresent();

        if (cedoDemais) {
            return 0;
        }

        final List<String> administradores = usuarioRepository.emailsDosAdministradores();

        if (administradores.isEmpty()) {
            // Sem reservar: os cadastros continuam por avisar, e o aviso sai quando houver a quem.
            log.error("[m=executarSeDevido] Ha cadastro aguardando aprovacao, mas nenhum administrador ativo "
                    + "para avisar. As contas continuam pendentes na tela de gerenciamento");
            return 0;
        }

        final List<CadastroPendente> pendentes = usuarioRepository.reservarPendentesParaAviso(agora);

        if (pendentes.isEmpty()) {
            return 0;
        }

        final String corpo = renderizar(pendentes, regras);
        final String assunto = pendentes.size() == 1
                ? "Cadastro aguardando aprovacao: %s".formatted(pendentes.getFirst().nome())
                : "%d cadastros aguardando aprovacao".formatted(pendentes.size());

        // Falha no aviso nao desfaz a reserva: os cadastros continuam na tela de contas e no
        // selo da lateral. Perder o e-mail atrasa a aprovacao; reenviar sem limite era o problema.
        administradores.forEach(destinatario -> {
            try {
                emailPort.enviar(new MensagemEmail(destinatario, assunto, corpo));
            } catch (final RuntimeException excecao) {
                log.error("[m=executarSeDevido] Falha ao avisar o administrador {}: {}",
                        destinatario, excecao.getMessage());
            }
        });

        log.info("[m=executarSeDevido] Aviso de {} cadastro(s) pendente(s) enviado a {} administrador(es)",
                pendentes.size(), administradores.size());

        return pendentes.size();
    }

    private String renderizar(final List<CadastroPendente> pendentes, final SegurancaProperties.Aprovacao regras) {
        final String urlBase = properties.notificacao().urlBase();
        final List<ItemDoAviso> listados = pendentes.stream()
                .limit(regras.avisoMaxListados())
                .map(pendente -> new ItemDoAviso(pendente.nome(), pendente.email(),
                        pendente.perfil().getDescricao(), pendente.perfil().getAtribuicoes(),
                        urlBase + "/aprovacao/" + pendente.tokenAprovacao() + "/aprovar",
                        urlBase + "/aprovacao/" + pendente.tokenAprovacao() + "/recusar"))
                .toList();
        final Context contexto = new Context();

        contexto.setVariable("cadastros", listados);
        contexto.setVariable("naoListados", pendentes.size() - listados.size());
        contexto.setVariable("validadeDias", regras.validadeDias());
        contexto.setVariable("linkContas", urlBase + "/usuarios");

        return templateEngine.process(TEMPLATE, contexto);
    }

    /**
     * Cadastro como aparece no e-mail.
     *
     * @param nome nome de quem se cadastrou
     * @param email e-mail de quem se cadastrou
     * @param perfil perfil solicitado
     * @param atribuicoes o que o perfil permite fazer
     * @param linkAprovar pagina de confirmacao da aprovacao
     * @param linkRecusar pagina de confirmacao da recusa
     */
    public record ItemDoAviso(String nome, String email, String perfil, String atribuicoes,
                              String linkAprovar, String linkRecusar) {
    }
}
