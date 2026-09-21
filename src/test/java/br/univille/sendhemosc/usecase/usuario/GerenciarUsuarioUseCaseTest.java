package br.univille.sendhemosc.usecase.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.univille.sendhemosc.domain.dto.UsuarioResumo;
import br.univille.sendhemosc.domain.enums.PerfilUsuario;
import br.univille.sendhemosc.domain.enums.SituacaoUsuario;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Cobre as duas travas do gerenciamento de contas. Sem elas, o administrador consegue se
 * trancar para fora do proprio sistema, e a saida seria mexer direto no banco.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Gerenciamento de contas")
class GerenciarUsuarioUseCaseTest {

    private static final String EU = "admin@example.org";
    private static final String OUTRO = "chefe@example.org";

    @Mock
    private IUsuarioRepositoryPort usuarioRepository;
    @Mock
    private IAuditoriaPort auditoria;

    private GerenciarUsuarioUseCase useCase;

    private GerenciarUsuarioUseCase criarUseCase() {
        if (useCase == null) {
            useCase = new GerenciarUsuarioUseCase(usuarioRepository, auditoria, new BCryptPasswordEncoder(4));
        }
        return useCase;
    }

    private UsuarioResumo conta(final Long id, final String email,
                                final PerfilUsuario perfil, final SituacaoUsuario situacao) {
        return new UsuarioResumo(id, "Fulano", email, perfil, situacao, LocalDateTime.now(), null);
    }

    @Nested
    @DisplayName("protecao contra acao sobre a propria conta")
    class SobreSiMesmo {

        @Test
        @DisplayName("nao permite excluir a propria conta")
        void naoExcluiASiMesmo() {
            when(usuarioRepository.buscarPorId(1L))
                    .thenReturn(Optional.of(conta(1L, EU, PerfilUsuario.MASTER, SituacaoUsuario.ATIVO)));

            assertThatThrownBy(() -> criarUseCase().excluir(1L, EU))
                    .isInstanceOf(NegocioException.class)
                    .hasMessage("usuario.acao-sobre-si");

            verify(usuarioRepository, never()).excluir(anyLong());
        }

        @Test
        @DisplayName("nao permite rebaixar o proprio perfil")
        void naoRebaixaASiMesmo() {
            when(usuarioRepository.buscarPorId(1L))
                    .thenReturn(Optional.of(conta(1L, EU, PerfilUsuario.MASTER, SituacaoUsuario.ATIVO)));
            when(usuarioRepository.existeComEmailDeOutro(anyString(), anyLong())).thenReturn(false);

            assertThatThrownBy(() -> criarUseCase()
                    .atualizar(1L, "Fulano", EU, PerfilUsuario.OPERADOR, SituacaoUsuario.ATIVO, EU))
                    .isInstanceOf(NegocioException.class)
                    .hasMessage("usuario.acao-sobre-si");
        }

        @Test
        @DisplayName("nao permite desativar a propria conta")
        void naoDesativaASiMesmo() {
            when(usuarioRepository.buscarPorId(1L))
                    .thenReturn(Optional.of(conta(1L, EU, PerfilUsuario.MASTER, SituacaoUsuario.ATIVO)));
            when(usuarioRepository.existeComEmailDeOutro(anyString(), anyLong())).thenReturn(false);

            assertThatThrownBy(() -> criarUseCase()
                    .atualizar(1L, "Fulano", EU, PerfilUsuario.MASTER, SituacaoUsuario.INATIVO, EU))
                    .isInstanceOf(NegocioException.class)
                    .hasMessage("usuario.acao-sobre-si");
        }

        @Test
        @DisplayName("permite alterar o proprio nome e e-mail, que nao tiram acesso")
        void permiteAlterarDadosProprios() {
            when(usuarioRepository.buscarPorId(1L))
                    .thenReturn(Optional.of(conta(1L, EU, PerfilUsuario.MASTER, SituacaoUsuario.ATIVO)));
            when(usuarioRepository.existeComEmailDeOutro(anyString(), anyLong())).thenReturn(false);
            when(usuarioRepository.atualizar(anyLong(), anyString(), anyString(), any(), any()))
                    .thenReturn(Optional.of(conta(1L, EU, PerfilUsuario.MASTER, SituacaoUsuario.ATIVO)));

            assertThatCode(() -> criarUseCase()
                    .atualizar(1L, "Novo Nome", EU, PerfilUsuario.MASTER, SituacaoUsuario.ATIVO, EU))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("protecao do ultimo administrador")
    class UltimoAdministrador {

        @Test
        @DisplayName("nao permite excluir o unico administrador ativo")
        void naoExcluiOUltimo() {
            when(usuarioRepository.buscarPorId(2L))
                    .thenReturn(Optional.of(conta(2L, OUTRO, PerfilUsuario.MASTER, SituacaoUsuario.ATIVO)));
            when(usuarioRepository.contarAdministradoresAtivos()).thenReturn(1L);

            assertThatThrownBy(() -> criarUseCase().excluir(2L, EU))
                    .isInstanceOf(NegocioException.class)
                    .hasMessage("usuario.ultimo-administrador");

            verify(usuarioRepository, never()).excluir(anyLong());
        }

        @Test
        @DisplayName("permite excluir um administrador quando ha outro ativo")
        void permiteQuandoHaOutro() {
            when(usuarioRepository.buscarPorId(2L))
                    .thenReturn(Optional.of(conta(2L, OUTRO, PerfilUsuario.MASTER, SituacaoUsuario.ATIVO)));
            when(usuarioRepository.contarAdministradoresAtivos()).thenReturn(2L);
            when(usuarioRepository.excluir(2L))
                    .thenReturn(Optional.of(conta(2L, OUTRO, PerfilUsuario.MASTER, SituacaoUsuario.ATIVO)));

            assertThatCode(() -> criarUseCase().excluir(2L, EU)).doesNotThrowAnyException();
            verify(usuarioRepository).excluir(2L);
        }

        @Test
        @DisplayName("excluir conta que nao e administrador nao depende dessa contagem")
        void operadorNaoDependeDaContagem() {
            when(usuarioRepository.buscarPorId(3L))
                    .thenReturn(Optional.of(conta(3L, OUTRO, PerfilUsuario.OPERADOR, SituacaoUsuario.ATIVO)));
            when(usuarioRepository.excluir(3L))
                    .thenReturn(Optional.of(conta(3L, OUTRO, PerfilUsuario.OPERADOR, SituacaoUsuario.ATIVO)));

            assertThatCode(() -> criarUseCase().excluir(3L, EU)).doesNotThrowAnyException();
            verify(usuarioRepository, never()).contarAdministradoresAtivos();
        }
    }

    @Nested
    @DisplayName("senha")
    class Senha {

        @Test
        @DisplayName("sem senha informada, gera uma e devolve para exibicao unica")
        void geraQuandoNaoInformada() {
            when(usuarioRepository.buscarPorId(1L))
                    .thenReturn(Optional.of(conta(1L, OUTRO, PerfilUsuario.OPERADOR, SituacaoUsuario.ATIVO)));
            when(usuarioRepository.trocarSenha(anyLong(), anyString()))
                    .thenReturn(Optional.of(conta(1L, OUTRO, PerfilUsuario.OPERADOR, SituacaoUsuario.ATIVO)));

            final var resultado = criarUseCase().redefinirSenha(1L, null);

            assertThat(resultado.senhaGerada()).isNotBlank();
        }

        @Test
        @DisplayName("com senha informada, nao devolve nada para exibir")
        void naoDevolveQuandoInformada() {
            when(usuarioRepository.buscarPorId(1L))
                    .thenReturn(Optional.of(conta(1L, OUTRO, PerfilUsuario.OPERADOR, SituacaoUsuario.ATIVO)));
            when(usuarioRepository.trocarSenha(anyLong(), anyString()))
                    .thenReturn(Optional.of(conta(1L, OUTRO, PerfilUsuario.OPERADOR, SituacaoUsuario.ATIVO)));

            assertThat(criarUseCase().redefinirSenha(1L, "senhaEscolhida").senhaGerada()).isNull();
        }

        @Test
        @DisplayName("a senha nunca vai para o registro de auditoria")
        void senhaNaoVaiParaAuditoria() {
            when(usuarioRepository.buscarPorId(1L))
                    .thenReturn(Optional.of(conta(1L, OUTRO, PerfilUsuario.OPERADOR, SituacaoUsuario.ATIVO)));
            when(usuarioRepository.trocarSenha(anyLong(), anyString()))
                    .thenReturn(Optional.of(conta(1L, OUTRO, PerfilUsuario.OPERADOR, SituacaoUsuario.ATIVO)));

            criarUseCase().redefinirSenha(1L, "senhaSuperSecreta");

            verify(auditoria).registrar(anyString(),
                    org.mockito.ArgumentMatchers.argThat(detalhe -> !detalhe.contains("senhaSuperSecreta")));
        }
    }

    @Test
    @DisplayName("e-mail ja usado por outra conta e recusado")
    void emailDuplicado() {
        when(usuarioRepository.buscarPorId(1L))
                .thenReturn(Optional.of(conta(1L, OUTRO, PerfilUsuario.OPERADOR, SituacaoUsuario.ATIVO)));
        when(usuarioRepository.existeComEmailDeOutro(anyString(), anyLong())).thenReturn(true);

        assertThatThrownBy(() -> criarUseCase()
                .atualizar(1L, "Fulano", "ocupado@example.org", PerfilUsuario.OPERADOR,
                        SituacaoUsuario.ATIVO, EU))
                .isInstanceOf(NegocioException.class)
                .hasMessage("usuario.email.duplicado");
    }
}
