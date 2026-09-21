package br.univille.sendhemosc.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Catalogo de erros do contexto de usuario e acesso.
 */
@Getter
@RequiredArgsConstructor
public enum UsuarioErrorsMessage implements ErrorsMessage {

    NAO_ENCONTRADO("USU-001", "usuario.nao-encontrado", HttpStatus.NOT_FOUND),
    EMAIL_DUPLICADO("USU-002", "usuario.email.duplicado", HttpStatus.CONFLICT),
    SENHAS_DIFERENTES("USU-003", "usuario.senhas-diferentes", HttpStatus.BAD_REQUEST),
    PERFIL_NAO_PERMITIDO("USU-004", "usuario.perfil.nao-permitido", HttpStatus.FORBIDDEN),
    APROVACAO_INVALIDA("USU-005", "usuario.aprovacao.invalida", HttpStatus.GONE),
    SEM_ADMINISTRADOR("USU-006", "usuario.sem-administrador", HttpStatus.SERVICE_UNAVAILABLE),
    ACAO_SOBRE_SI("USU-007", "usuario.acao-sobre-si", HttpStatus.CONFLICT),
    ULTIMO_ADMINISTRADOR("USU-008", "usuario.ultimo-administrador", HttpStatus.CONFLICT);

    private final String codigo;
    private final String chaveMensagem;
    private final HttpStatus status;
}
