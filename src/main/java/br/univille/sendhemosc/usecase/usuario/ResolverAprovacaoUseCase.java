package br.univille.sendhemosc.usecase.usuario;

import br.univille.sendhemosc.domain.dto.UsuarioResumo;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.exception.UsuarioErrorsMessage;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Aprova ou recusa um cadastro pendente, pelo link recebido por e-mail ou pela tela de
 * gerenciamento. O token e de uso unico: abrir o mesmo link duas vezes resulta em erro, e nao
 * em nova decisao.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResolverAprovacaoUseCase {

    private final IUsuarioRepositoryPort usuarioRepository;
    private final IAuditoriaPort auditoria;

    /**
     * Decide a partir do token recebido por e-mail.
     *
     * @param token token do link
     * @param aprovado true para liberar a conta
     * @return o usuario afetado
     */
    public UsuarioResumo porToken(final String token, final boolean aprovado) {
        final UsuarioResumo usuario = usuarioRepository.resolverAprovacaoPorToken(token, aprovado, null)
                .orElseThrow(() -> new NegocioException(UsuarioErrorsMessage.APROVACAO_INVALIDA));

        return registrar(usuario, aprovado);
    }

    /**
     * Decide a partir da tela de gerenciamento.
     *
     * @param usuarioId conta a decidir
     * @param aprovado true para liberar a conta
     * @param aprovadorId identificador de quem decidiu
     * @return o usuario afetado
     */
    public UsuarioResumo porTela(final Long usuarioId, final boolean aprovado, final Long aprovadorId) {
        final UsuarioResumo usuario = usuarioRepository.resolverAprovacao(usuarioId, aprovado, aprovadorId)
                .orElseThrow(() -> new NegocioException(UsuarioErrorsMessage.APROVACAO_INVALIDA));

        return registrar(usuario, aprovado);
    }

    private UsuarioResumo registrar(final UsuarioResumo usuario, final boolean aprovado) {
        auditoria.registrar(aprovado ? "USUARIO_APROVADO" : "USUARIO_RECUSADO",
                "%s (%s) como %s".formatted(usuario.nome(), usuario.email(), usuario.perfil()));

        return usuario;
    }
}
