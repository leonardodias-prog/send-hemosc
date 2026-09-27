package br.univille.sendhemosc.usecase.usuario;

import br.univille.sendhemosc.domain.dto.UsuarioAutenticavel;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.exception.UsuarioErrorsMessage;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import br.univille.sendhemosc.domain.port.outbound.IRecuperacaoSenhaPort;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Troca da senha pela propria pessoa, ja autenticada.
 *
 * <p>Pede a senha atual mesmo de quem ja entrou: sessao aberta num computador esquecido nao
 * basta para tomar a conta. A redefinicao pelo administrador continua existindo, para quem
 * esqueceu a senha e nao consegue usar o link por e-mail.</p>
 */
@Service
@RequiredArgsConstructor
public class TrocarPropriaSenhaUseCase {

    private final IUsuarioRepositoryPort usuarioRepository;
    private final IRecuperacaoSenhaPort recuperacaoSenha;
    private final IAuditoriaPort auditoria;
    private final PasswordEncoder passwordEncoder;

    /**
     * Troca a senha de quem esta autenticado.
     *
     * @param email e-mail da conta autenticada
     * @param senhaAtual senha em uso, conferida antes de qualquer mudanca
     * @param novaSenha senha nova
     * @param confirmacao repeticao da senha nova
     */
    public void execute(final String email, final String senhaAtual,
                        final String novaSenha, final String confirmacao) {
        final UsuarioAutenticavel usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new NegocioException(UsuarioErrorsMessage.NAO_ENCONTRADO));

        if (senhaAtual == null || RegraDeSenha.excedeOLimite(senhaAtual)
                || !passwordEncoder.matches(senhaAtual, usuario.senhaHash())) {
            throw new NegocioException(UsuarioErrorsMessage.SENHA_ATUAL_INCORRETA);
        }

        RegraDeSenha.validar(novaSenha, confirmacao);

        usuarioRepository.trocarSenha(usuario.id(), passwordEncoder.encode(novaSenha))
                .orElseThrow(() -> new NegocioException(UsuarioErrorsMessage.NAO_ENCONTRADO));

        // Link de recuperacao pedido antes deixa de valer: a senha que ele trocaria ja mudou.
        recuperacaoSenha.invalidarAbertos(usuario.id(), LocalDateTime.now());

        // A senha nao vai para o registro: ele guarda apenas que houve troca, e por quem.
        auditoria.registrar("SENHA_ALTERADA", "%s (%s)".formatted(usuario.nome(), usuario.email()));
    }
}
