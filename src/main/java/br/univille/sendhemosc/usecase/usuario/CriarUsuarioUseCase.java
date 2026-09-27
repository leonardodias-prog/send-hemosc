package br.univille.sendhemosc.usecase.usuario;

import br.univille.sendhemosc.domain.dto.NovoUsuario;
import br.univille.sendhemosc.domain.enums.PerfilUsuario;
import br.univille.sendhemosc.domain.enums.SituacaoUsuario;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.exception.UsuarioErrorsMessage;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Cadastro de funcionario feito pela propria pessoa.
 *
 * <p>Operador entra ativo na hora, porque apenas alimenta dados. Responsavel entra pendente e
 * depende de aprovacao, porque o perfil permite disparar e-mail para a base de doadores, efeito
 * que sai do sistema e nao volta. Administrador nao pode ser escolhido aqui de forma alguma.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CriarUsuarioUseCase {

    private final IUsuarioRepositoryPort usuarioRepository;
    private final AvisarCadastrosPendentesUseCase avisarCadastrosPendentes;
    private final IAuditoriaPort auditoria;
    private final PasswordEncoder passwordEncoder;

    /**
     * Executa o cadastro.
     *
     * @param novoUsuario dados informados na tela
     * @return resultado, indicando se a conta ja esta liberada ou aguarda aprovacao
     */
    public Resultado execute(final NovoUsuario novoUsuario) {
        if (!novoUsuario.perfil().disponivelNoAutocadastro()) {
            throw new NegocioException(UsuarioErrorsMessage.PERFIL_NAO_PERMITIDO);
        }
        if (!novoUsuario.senha().equals(novoUsuario.confirmacaoSenha())) {
            throw new NegocioException(UsuarioErrorsMessage.SENHAS_DIFERENTES);
        }

        final String email = novoUsuario.email().trim().toLowerCase();
        if (usuarioRepository.existeComEmail(email)) {
            throw new NegocioException(UsuarioErrorsMessage.EMAIL_DUPLICADO);
        }

        final boolean dependeDeAprovacao = novoUsuario.perfil().exigeAprovacao();
        final String token = dependeDeAprovacao ? UUID.randomUUID().toString() : null;

        usuarioRepository.criar(
                novoUsuario.nome().trim(),
                email,
                passwordEncoder.encode(novoUsuario.senha()),
                novoUsuario.perfil(),
                dependeDeAprovacao ? SituacaoUsuario.PENDENTE : SituacaoUsuario.ATIVO,
                token);

        auditoria.registrar("USUARIO_CADASTRADO",
                "%s (%s) como %s".formatted(novoUsuario.nome(), email, novoUsuario.perfil()));

        if (dependeDeAprovacao) {
            avisarAdministradores();
        }

        log.info("[m=execute] Conta criada para {} com perfil {}, aguardando aprovacao={}",
                email, novoUsuario.perfil(), dependeDeAprovacao);

        return new Resultado(dependeDeAprovacao, novoUsuario.perfil());
    }

    /**
     * Pede o aviso aos administradores. Falha aqui nao derruba o cadastro: a conta ja existe como
     * pendente, aparece na tela de contas, e o agendador tenta o aviso de novo na conferencia
     * seguinte. Perder o aviso atrasa a aprovacao; perder o cadastro obrigaria a pessoa a se
     * inscrever de novo.
     */
    private void avisarAdministradores() {
        try {
            avisarCadastrosPendentes.executarSeDevido(LocalDateTime.now());
        } catch (final RuntimeException excecao) {
            log.error("[m=execute] Falha ao avisar os administradores do cadastro pendente", excecao);
        }
    }

    /**
     * Desfecho do cadastro.
     *
     * @param aguardandoAprovacao true quando a conta so sera liberada apos decisao de um administrador
     * @param perfil perfil solicitado
     */
    public record Resultado(boolean aguardandoAprovacao, PerfilUsuario perfil) {
    }
}
