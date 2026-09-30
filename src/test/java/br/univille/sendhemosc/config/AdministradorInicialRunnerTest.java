package br.univille.sendhemosc.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.univille.sendhemosc.domain.enums.PerfilUsuario;
import br.univille.sendhemosc.domain.enums.SituacaoUsuario;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;

@DisplayName("Administrador inicial")
class AdministradorInicialRunnerTest {

    private static final String EMAIL = "Admin@Exemplo.org";

    private IUsuarioRepositoryPort usuarioRepository;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void preparar() {
        usuarioRepository = mock(IUsuarioRepositoryPort.class);
        passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode(any())).thenReturn("hash");
    }

    private AdministradorInicialRunner runner(final String perfil, final String senha) {
        final MockEnvironment ambiente = new MockEnvironment();
        ambiente.setActiveProfiles(perfil);

        return new AdministradorInicialRunner(usuarioRepository, passwordEncoder, ambiente, EMAIL, "Admin", senha);
    }

    @Test
    @DisplayName("producao sem MASTER_SENHA e sem administrador: o servico se recusa a subir")
    void producaoSemSenhaNaoSobe() {
        when(usuarioRepository.existeAlgumComPerfil(PerfilUsuario.MASTER)).thenReturn(false);

        assertThatThrownBy(() -> runner("producao", "").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MASTER_SENHA");

        verify(usuarioRepository, never()).criar(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("producao com MASTER_SENHA cria o administrador ativo")
    void producaoComSenhaCria() {
        when(usuarioRepository.existeAlgumComPerfil(PerfilUsuario.MASTER)).thenReturn(false);

        runner("producao", "senha-do-admin-123").run(null);

        verify(passwordEncoder).encode("senha-do-admin-123");
        verify(usuarioRepository).criar(eq("Admin"), eq("admin@exemplo.org"), eq("hash"),
                eq(PerfilUsuario.MASTER), eq(SituacaoUsuario.ATIVO), any());
    }

    @Test
    @DisplayName("producao com administrador ja criado sobe mesmo sem MASTER_SENHA")
    void producaoComAdministradorExistenteSobe() {
        when(usuarioRepository.existeAlgumComPerfil(PerfilUsuario.MASTER)).thenReturn(true);

        runner("producao", "").run(null);

        verify(usuarioRepository, never()).criar(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("em desenvolvimento, sem MASTER_SENHA, gera uma senha aleatoria")
    void desenvolvimentoGeraSenha() {
        when(usuarioRepository.existeAlgumComPerfil(PerfilUsuario.MASTER)).thenReturn(false);

        runner("dev", "").run(null);

        verify(usuarioRepository).criar(any(), any(), eq("hash"), eq(PerfilUsuario.MASTER),
                eq(SituacaoUsuario.ATIVO), any());
    }
}
