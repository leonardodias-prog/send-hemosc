package br.univille.sendhemosc.usecase.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.univille.sendhemosc.config.SegurancaProperties;
import br.univille.sendhemosc.config.SendHemoscProperties;
import br.univille.sendhemosc.domain.dto.MensagemEmail;
import br.univille.sendhemosc.domain.dto.UsuarioAutenticavel;
import br.univille.sendhemosc.domain.enums.PerfilUsuario;
import br.univille.sendhemosc.domain.enums.SituacaoUsuario;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import br.univille.sendhemosc.domain.port.outbound.IEmailPort;
import br.univille.sendhemosc.domain.port.outbound.IRecuperacaoSenhaPort;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Pedido de recuperacao de senha")
class SolicitarRecuperacaoDeSenhaUseCaseTest {

    private static final UsuarioAutenticavel ATIVA = new UsuarioAutenticavel(7L, "Pessoa", "pessoa@example.org",
            "hash", PerfilUsuario.OPERADOR, SituacaoUsuario.ATIVO);

    @Mock
    private IUsuarioRepositoryPort usuarioRepository;

    @Mock
    private IRecuperacaoSenhaPort recuperacaoSenha;

    @Mock
    private IEmailPort emailPort;

    @Mock
    private IAuditoriaPort auditoria;

    @Mock
    private TemplateEngine templateEngine;

    private SolicitarRecuperacaoDeSenhaUseCase useCase;

    @BeforeEach
    void preparar() {
        final SendHemoscProperties properties = new SendHemoscProperties(null, null,
                new SendHemoscProperties.Notificacao(30, 3, 180, "r@example.org", "Remetente", "https://app.example.org"));
        final SegurancaProperties seguranca = new SegurancaProperties(
                new SegurancaProperties.Login(5, 15),
                new SegurancaProperties.RecuperacaoSenha(60, 15, 3, 30),
                new SegurancaProperties.Aprovacao(7, 60, 10));

        useCase = new SolicitarRecuperacaoDeSenhaUseCase(usuarioRepository, recuperacaoSenha, emailPort,
                auditoria, templateEngine, properties, seguranca);

        when(usuarioRepository.buscarPorEmail("pessoa@example.org")).thenReturn(Optional.of(ATIVA));
        when(templateEngine.process(anyString(), any(IContext.class))).thenReturn("<p>corpo</p>");
        when(recuperacaoSenha.ultimoDaConta(7L)).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("conta ativa recebe um link, e o banco guarda o hash, nunca o token")
    void contaAtivaRecebeLink() {
        useCase.execute("  Pessoa@Example.org ");

        final ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(recuperacaoSenha).registrar(eq(7L), hash.capture(), any(LocalDateTime.class), any(LocalDateTime.class));
        final ArgumentCaptor<MensagemEmail> mensagem = ArgumentCaptor.forClass(MensagemEmail.class);
        verify(emailPort).enviar(mensagem.capture());

        assertThat(mensagem.getValue().destinatario()).isEqualTo("pessoa@example.org");
        assertThat(hash.getValue()).hasSize(64).matches("[0-9a-f]+");
        verify(recuperacaoSenha).invalidarAbertos(eq(7L), any(LocalDateTime.class));
        verify(auditoria).registrar(eq("RECUPERACAO_DE_SENHA_PEDIDA"), anyString());
    }

    @Test
    @DisplayName("e-mail sem conta nao recebe nada, e nada fica gravado")
    void emailSemConta() {
        useCase.execute("ninguem@example.org");

        verify(emailPort, never()).enviar(any());
        verify(recuperacaoSenha, never()).registrar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("conta pendente, recusada ou inativa tambem nao: senha nova nao a faria entrar")
    void contaSemAcesso() {
        when(usuarioRepository.buscarPorEmail("pendente@example.org")).thenReturn(Optional.of(new UsuarioAutenticavel(
                8L, "Pendente", "pendente@example.org", "hash", PerfilUsuario.RESPONSAVEL, SituacaoUsuario.PENDENTE)));

        useCase.execute("pendente@example.org");

        verify(emailPort, never()).enviar(any());
    }

    @Test
    @DisplayName("pedido de novo antes do intervalo nao manda outro e-mail")
    void intervaloEntrePedidos() {
        when(recuperacaoSenha.ultimoDaConta(7L)).thenReturn(Optional.of(LocalDateTime.now().minusMinutes(5)));

        useCase.execute("pessoa@example.org");

        verify(emailPort, never()).enviar(any());
    }

    @Test
    @DisplayName("tres links em 24 horas e o teto da conta")
    void tetoDaConta() {
        when(recuperacaoSenha.contarDaConta(eq(7L), any(LocalDateTime.class))).thenReturn(3L);

        useCase.execute("pessoa@example.org");

        verify(emailPort, never()).enviar(any());
    }

    @Test
    @DisplayName("o teto do sistema inteiro protege a cota do provedor, mesmo com contas diferentes")
    void tetoDoSistema() {
        when(recuperacaoSenha.contarTodos(any(LocalDateTime.class))).thenReturn(30L);

        useCase.execute("pessoa@example.org");

        verify(emailPort, never()).enviar(any());
        verify(recuperacaoSenha, never()).registrar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("falha no envio nao vira erro para quem pediu, e nao entra na auditoria")
    void falhaNoEnvio() {
        org.mockito.Mockito.doThrow(new IllegalStateException("provedor fora")).when(emailPort).enviar(any());

        useCase.execute("pessoa@example.org");

        verify(auditoria, never()).registrar(anyString(), anyString());
    }

    @Test
    @DisplayName("e-mail vazio nao consulta nada")
    void emailVazio() {
        useCase.execute("   ");

        verify(emailPort, never()).enviar(any());
        verify(usuarioRepository, never()).buscarPorEmail(anyString());
    }
}
