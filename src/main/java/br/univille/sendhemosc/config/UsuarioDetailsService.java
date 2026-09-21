package br.univille.sendhemosc.config;

import br.univille.sendhemosc.domain.dto.UsuarioAutenticavel;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Liga a autenticacao do Spring Security a tabela de usuarios.
 *
 * <p>A conta so autentica quando a situacao permite. Cadastro de responsavel entra pendente e
 * so passa a valer depois da decisao de um administrador; conta recusada ou desativada e
 * tratada como desabilitada, e nao como senha errada, para que o motivo apareca no log.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

    private final IUsuarioRepositoryPort usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(final String email) {
        final UsuarioAutenticavel usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Conta nao encontrada"));

        if (!usuario.situacao().permiteAcesso()) {
            log.warn("[m=loadUserByUsername] Tentativa de acesso de {} com conta em situacao {}",
                    email, usuario.situacao());
        }

        return User.withUsername(usuario.email())
                .password(usuario.senhaHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + usuario.perfil().name())))
                .disabled(!usuario.situacao().permiteAcesso())
                .build();
    }
}
