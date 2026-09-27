package br.univille.sendhemosc.usecase.usuario;

import br.univille.sendhemosc.domain.dto.PedidoDeRecuperacao;
import br.univille.sendhemosc.domain.dto.UsuarioResumo;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.exception.UsuarioErrorsMessage;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import br.univille.sendhemosc.domain.port.outbound.IRecuperacaoSenhaPort;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Cria a senha nova a partir do link recebido por e-mail.
 *
 * <p>Abrir o link nao muda nada: so mostra o formulario. Filtros de e-mail que abrem links
 * sozinhos para inspecionar nao gastam o link nem trocam senha de ninguem. A troca acontece no
 * envio do formulario, que so a pessoa faz.</p>
 */
@Service
@RequiredArgsConstructor
public class RedefinirSenhaPorLinkUseCase {

    private final IRecuperacaoSenhaPort recuperacaoSenha;
    private final IUsuarioRepositoryPort usuarioRepository;
    private final IAuditoriaPort auditoria;
    private final PasswordEncoder passwordEncoder;
    private final LimiteDeTentativasDeLogin limiteDeTentativas;

    /**
     * Consulta o link sem usa-lo: e o que a pagina mostra ao abrir.
     *
     * @param token token do link
     * @return a conta a que o link pertence, vazio quando ele ja nao serve
     */
    public Optional<UsuarioResumo> consultar(final String token) {
        return pedidoAberto(token, LocalDateTime.now()).flatMap(pedido -> contaAtiva(pedido.usuarioId()));
    }

    /**
     * Grava a senha nova e fecha o link.
     *
     * @param token token do link
     * @param novaSenha senha nova
     * @param confirmacao repeticao da senha nova
     * @return a conta que teve a senha redefinida
     */
    public UsuarioResumo execute(final String token, final String novaSenha, final String confirmacao) {
        // A regra vem antes do link: um erro de digitacao nao pode gastar o link.
        RegraDeSenha.validar(novaSenha, confirmacao);

        final LocalDateTime agora = LocalDateTime.now();
        final PedidoDeRecuperacao pedido = pedidoAberto(token, agora).orElseThrow(RedefinirSenhaPorLinkUseCase::invalido);
        final UsuarioResumo usuario = contaAtiva(pedido.usuarioId()).orElseThrow(RedefinirSenhaPorLinkUseCase::invalido);

        // Fechamento condicional: o mesmo link enviado em duas abas troca a senha uma vez so.
        if (!recuperacaoSenha.usar(pedido.id(), agora)) {
            throw invalido();
        }

        usuarioRepository.trocarSenha(usuario.id(), passwordEncoder.encode(novaSenha))
                .orElseThrow(RedefinirSenhaPorLinkUseCase::invalido);
        recuperacaoSenha.invalidarAbertos(usuario.id(), agora);

        // Quem estava bloqueado por errar a senha acaba de provar que controla o e-mail da conta.
        limiteDeTentativas.liberar(usuario.email());

        auditoria.registrar("SENHA_REDEFINIDA_PELO_LINK", "%s (%s)".formatted(usuario.nome(), usuario.email()));

        return usuario;
    }

    private Optional<PedidoDeRecuperacao> pedidoAberto(final String token, final LocalDateTime agora) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        return recuperacaoSenha.buscarPorTokenHash(TokenDeRecuperacao.hash(token))
                .filter(pedido -> pedido.aberto(agora));
    }

    private Optional<UsuarioResumo> contaAtiva(final Long usuarioId) {
        return usuarioRepository.buscarPorId(usuarioId).filter(usuario -> usuario.situacao().permiteAcesso());
    }

    private static NegocioException invalido() {
        return new NegocioException(UsuarioErrorsMessage.RECUPERACAO_INVALIDA);
    }
}
