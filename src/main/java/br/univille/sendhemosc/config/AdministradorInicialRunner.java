package br.univille.sendhemosc.config;

import br.univille.sendhemosc.domain.enums.PerfilUsuario;
import br.univille.sendhemosc.domain.enums.SituacaoUsuario;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import java.security.SecureRandom;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Garante que exista um administrador desde a primeira subida.
 *
 * <p>Sem ele ninguem conseguiria aprovar o primeiro responsavel, e o sistema ficaria travado.
 * A conta e criada apenas quando nao ha nenhum administrador: em base ja povoada, nada
 * acontece, e trocar a variavel de senha nao redefine a senha de quem ja existe.</p>
 *
 * <p>Quando a senha nao e configurada, uma aleatoria e gerada e escrita no log. Isso evita
 * senha padrao conhecida no codigo, que seria a mesma em toda instalacao.</p>
 */
@Slf4j
@Component
@Order(1)
public class AdministradorInicialRunner implements ApplicationRunner {

    private final IUsuarioRepositoryPort usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String nome;
    private final String senhaConfigurada;

    public AdministradorInicialRunner(
            final IUsuarioRepositoryPort usuarioRepository,
            final PasswordEncoder passwordEncoder,
            @Value("${sendhemosc.seguranca.master.email:admin@sendhemosc.local}") final String email,
            @Value("${sendhemosc.seguranca.master.nome:Administrador}") final String nome,
            @Value("${sendhemosc.seguranca.master.senha:}") final String senhaConfigurada) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.nome = nome;
        this.senhaConfigurada = senhaConfigurada;
    }

    @Override
    public void run(final ApplicationArguments args) {
        if (usuarioRepository.existeAlgumComPerfil(PerfilUsuario.MASTER)) {
            log.debug("[m=run] Ja existe administrador, nada a criar");
            return;
        }

        final boolean senhaGerada = !StringUtils.hasText(senhaConfigurada);
        final String senha = senhaGerada ? gerarSenha() : senhaConfigurada;

        usuarioRepository.criar(nome, email.toLowerCase(), passwordEncoder.encode(senha),
                PerfilUsuario.MASTER, SituacaoUsuario.ATIVO, null);

        if (senhaGerada) {
            log.warn("""

                    ===========================================================================
                     ADMINISTRADOR CRIADO

                     E-mail : {}
                     Senha  : {}

                     Senha gerada porque MASTER_SENHA nao foi definida. Ela aparece apenas
                     nesta subida e nao volta a ser exibida. Defina MASTER_SENHA para ter
                     uma senha estavel entre reinicios.
                    ===========================================================================
                    """, email, senha);
        } else {
            log.info("[m=run] Administrador criado para {} com a senha configurada", email);
        }
    }

    private String gerarSenha() {
        final byte[] bytes = new byte[12];
        new SecureRandom().nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
