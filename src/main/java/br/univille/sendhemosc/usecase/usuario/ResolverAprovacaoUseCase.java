package br.univille.sendhemosc.usecase.usuario;

import br.univille.sendhemosc.config.SegurancaProperties;
import br.univille.sendhemosc.domain.dto.CadastroPendente;
import br.univille.sendhemosc.domain.dto.UsuarioResumo;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.exception.UsuarioErrorsMessage;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Aprova ou recusa um cadastro pendente, pelo link recebido por e-mail ou pela tela de
 * gerenciamento.
 *
 * <p>Pelo link, a decisao acontece em dois passos: abrir o link so mostra o cadastro, e a
 * decisao sai do botao da pagina. Filtros de e-mail corporativos abrem links sozinhos para
 * inspecionar; se abrir bastasse, um responsavel podia ser aprovado, e ganhar o poder de mandar
 * e-mail para a base de doadores, sem ninguem clicar. O link vale uma vez e por um prazo contado
 * do cadastro; vencido, a decisao e pela tela de contas.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResolverAprovacaoUseCase {

    private final IUsuarioRepositoryPort usuarioRepository;
    private final IAuditoriaPort auditoria;
    private final SegurancaProperties seguranca;

    /**
     * Consulta o cadastro do link sem decidir nada: e o que a pagina de confirmacao mostra.
     *
     * @param token token do link
     * @return o cadastro pendente, vazio quando o link nao existe ou ja foi usado
     */
    public Optional<CadastroPendente> consultarPorToken(final String token) {
        return usuarioRepository.buscarPendentePorToken(token);
    }

    /**
     * Indica se o link do cadastro ja passou do prazo.
     *
     * @param cadastro cadastro pendente
     * @return true quando o link nao vale mais
     */
    public boolean linkVencido(final CadastroPendente cadastro) {
        return cadastro.linkVencido(LocalDateTime.now(), seguranca.aprovacao().validadeDias());
    }

    /**
     * Decide a partir do botao da pagina aberta pelo link do e-mail.
     *
     * @param token token do link
     * @param aprovado true para liberar a conta
     * @return o usuario afetado
     */
    public UsuarioResumo porToken(final String token, final boolean aprovado) {
        final CadastroPendente pendente = usuarioRepository.buscarPendentePorToken(token)
                .orElseThrow(() -> new NegocioException(UsuarioErrorsMessage.APROVACAO_INVALIDA));

        if (linkVencido(pendente)) {
            throw new NegocioException(UsuarioErrorsMessage.APROVACAO_VENCIDA);
        }

        final UsuarioResumo usuario = usuarioRepository.resolverAprovacaoPorToken(token, aprovado, null)
                .orElseThrow(() -> new NegocioException(UsuarioErrorsMessage.APROVACAO_INVALIDA));

        return registrar(usuario, aprovado);
    }

    /**
     * Decide a partir da tela de gerenciamento. Nao depende do prazo do link: quem esta na tela
     * ja e o administrador autenticado.
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
