package br.univille.sendhemosc.usecase.usuario;

import br.univille.sendhemosc.config.SegurancaProperties;
import br.univille.sendhemosc.config.SendHemoscProperties;
import br.univille.sendhemosc.domain.dto.MensagemEmail;
import br.univille.sendhemosc.domain.dto.UsuarioAutenticavel;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import br.univille.sendhemosc.domain.port.outbound.IEmailPort;
import br.univille.sendhemosc.domain.port.outbound.IRecuperacaoSenhaPort;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Atende o "esqueci minha senha": manda por e-mail um link que permite criar uma senha nova.
 *
 * <p>Nao devolve nada, de proposito. A tela diz a mesma coisa exista a conta ou nao, para nao
 * revelar a quem digita quais e-mails tem cadastro.</p>
 *
 * <p>Todo pedido atendido vira um e-mail, entao ha tres limites: intervalo minimo e teto diario
 * por conta, contra quem enche a caixa de alguem de pedidos, e teto diario do sistema inteiro,
 * contra quem cria contas no autocadastro so para pedir links e esgotar a cota do provedor, o
 * que deixaria as convocacoes sem envio.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SolicitarRecuperacaoDeSenhaUseCase {

    private static final String TEMPLATE = "email/recuperacao-senha";
    private static final String ASSUNTO = "Redefinicao de senha no Send Hemosc";

    /** Pedidos mais antigos que isto ja nao contam para limite nenhum e saem do banco. */
    private static final int DIAS_DE_HISTORICO = 2;

    private final IUsuarioRepositoryPort usuarioRepository;
    private final IRecuperacaoSenhaPort recuperacaoSenha;
    private final IEmailPort emailPort;
    private final IAuditoriaPort auditoria;
    private final TemplateEngine templateEngine;
    private final SendHemoscProperties properties;
    private final SegurancaProperties seguranca;

    /**
     * Processa o pedido.
     *
     * @param emailInformado e-mail digitado na tela
     */
    public void execute(final String emailInformado) {
        final LocalDateTime agora = LocalDateTime.now();
        recuperacaoSenha.apagarAnterioresA(agora.minusDays(DIAS_DE_HISTORICO));

        final Optional<UsuarioAutenticavel> conta = Optional.ofNullable(emailInformado)
                .map(email -> email.trim().toLowerCase(Locale.ROOT))
                .filter(email -> !email.isEmpty())
                .flatMap(usuarioRepository::buscarPorEmail)
                .filter(usuario -> usuario.situacao().permiteAcesso());

        if (conta.isEmpty()) {
            log.info("[m=execute] Pedido de recuperacao sem conta ativa correspondente: nada enviado");
            return;
        }

        final UsuarioAutenticavel usuario = conta.get();
        final SegurancaProperties.RecuperacaoSenha regras = seguranca.recuperacaoSenha();

        if (!cabeNosLimites(usuario, regras, agora)) {
            return;
        }

        final String token = TokenDeRecuperacao.gerar();

        // So o link mais recente vale: pedir de novo substitui o anterior.
        recuperacaoSenha.invalidarAbertos(usuario.id(), agora);
        recuperacaoSenha.registrar(usuario.id(), TokenDeRecuperacao.hash(token), agora,
                agora.plusMinutes(regras.validadeMinutos()));

        try {
            emailPort.enviar(new MensagemEmail(usuario.email(), ASSUNTO, renderizar(usuario, token, regras)));
        } catch (final RuntimeException excecao) {
            log.error("[m=execute] Falha ao enviar o link de recuperacao da conta {}: {}",
                    usuario.id(), excecao.getMessage());
            return;
        }

        auditoria.registrar("RECUPERACAO_DE_SENHA_PEDIDA", "%s (%s)".formatted(usuario.nome(), usuario.email()));
    }

    private boolean cabeNosLimites(final UsuarioAutenticavel usuario,
                                   final SegurancaProperties.RecuperacaoSenha regras,
                                   final LocalDateTime agora) {
        final LocalDateTime umDiaAtras = agora.minusDays(1);

        if (recuperacaoSenha.contarTodos(umDiaAtras) >= regras.maxNoSistemaDia()) {
            log.warn("[m=execute] Teto diario de links de recuperacao atingido ({}): pedido da conta {} nao enviado",
                    regras.maxNoSistemaDia(), usuario.id());
            return false;
        }

        if (recuperacaoSenha.contarDaConta(usuario.id(), umDiaAtras) >= regras.maxPorContaDia()) {
            log.info("[m=execute] Conta {} ja recebeu {} links nas ultimas 24 horas: nada enviado",
                    usuario.id(), regras.maxPorContaDia());
            return false;
        }

        final boolean pedidoRecente = recuperacaoSenha.ultimoDaConta(usuario.id())
                .filter(ultimo -> ultimo.isAfter(agora.minusMinutes(regras.intervaloMinutos())))
                .isPresent();

        if (pedidoRecente) {
            log.info("[m=execute] Conta {} pediu um link ha menos de {} minutos: nada enviado",
                    usuario.id(), regras.intervaloMinutos());
            return false;
        }

        return true;
    }

    private String renderizar(final UsuarioAutenticavel usuario, final String token,
                              final SegurancaProperties.RecuperacaoSenha regras) {
        final Context contexto = new Context();

        contexto.setVariable("nome", usuario.nome());
        contexto.setVariable("email", usuario.email());
        contexto.setVariable("link", properties.notificacao().urlBase() + "/senha/redefinir/" + token);
        contexto.setVariable("validadeMinutos", regras.validadeMinutos());

        return templateEngine.process(TEMPLATE, contexto);
    }
}
