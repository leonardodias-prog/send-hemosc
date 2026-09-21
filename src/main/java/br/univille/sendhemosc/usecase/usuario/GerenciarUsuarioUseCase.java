package br.univille.sendhemosc.usecase.usuario;

import br.univille.sendhemosc.domain.dto.UsuarioResumo;
import br.univille.sendhemosc.domain.enums.PerfilUsuario;
import br.univille.sendhemosc.domain.enums.SituacaoUsuario;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.exception.UsuarioErrorsMessage;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import java.security.SecureRandom;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Gerenciamento de contas pelo administrador: criar, alterar, redefinir senha e excluir.
 *
 * <p>Duas travas existem aqui, e nenhuma e detalhe. A primeira impede que o administrador
 * altere o proprio perfil, desative ou exclua a propria conta: e o caminho mais comum de se
 * trancar para fora do sistema. A segunda impede remover ou rebaixar o ultimo administrador
 * ativo, o que deixaria o sistema sem ninguem capaz de aprovar cadastros nem gerenciar
 * contas, sem outra saida a nao ser mexer direto no banco.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GerenciarUsuarioUseCase {

    private final IUsuarioRepositoryPort usuarioRepository;
    private final IAuditoriaPort auditoria;
    private final PasswordEncoder passwordEncoder;

    /**
     * Cria uma conta ja ativa, com qualquer perfil. Diferente do autocadastro, aqui nao ha
     * aprovacao pendente: quem cria e o administrador, que ja e a autoridade que aprovaria.
     *
     * @param nome nome completo
     * @param email endereco de e-mail
     * @param senha senha em texto, convertida em hash antes de qualquer persistencia
     * @param perfil perfil da conta
     * @return a conta criada
     */
    public UsuarioResumo criar(final String nome, final String email,
                               final String senha, final PerfilUsuario perfil) {
        final String emailNormalizado = email.trim().toLowerCase();

        if (usuarioRepository.existeComEmail(emailNormalizado)) {
            throw new NegocioException(UsuarioErrorsMessage.EMAIL_DUPLICADO);
        }

        final Long id = usuarioRepository.criar(nome.trim(), emailNormalizado,
                passwordEncoder.encode(senha), perfil, SituacaoUsuario.ATIVO, null);

        auditoria.registrar("CONTA_CRIADA", "%s (%s) como %s".formatted(nome, emailNormalizado, perfil));

        return usuarioRepository.buscarPorId(id)
                .orElseThrow(() -> new NegocioException(UsuarioErrorsMessage.NAO_ENCONTRADO));
    }

    /**
     * Altera nome, e-mail, perfil e situacao de uma conta.
     *
     * @param id conta a alterar
     * @param nome novo nome
     * @param email novo e-mail
     * @param perfil novo perfil
     * @param situacao nova situacao
     * @param emailDeQuemEdita e-mail do administrador que esta editando
     * @return a conta atualizada
     */
    public UsuarioResumo atualizar(final Long id, final String nome, final String email,
                                   final PerfilUsuario perfil, final SituacaoUsuario situacao,
                                   final String emailDeQuemEdita) {
        final UsuarioResumo atual = exigirExistente(id);
        final String emailNormalizado = email.trim().toLowerCase();

        if (usuarioRepository.existeComEmailDeOutro(emailNormalizado, id)) {
            throw new NegocioException(UsuarioErrorsMessage.EMAIL_DUPLICADO);
        }

        final boolean perdeuPoder = atual.perfil() == PerfilUsuario.MASTER
                && (perfil != PerfilUsuario.MASTER || situacao != SituacaoUsuario.ATIVO);

        if (perdeuPoder) {
            impedirAcaoSobreSi(atual, emailDeQuemEdita);
            exigirOutroAdministrador();
        }

        final UsuarioResumo atualizado = usuarioRepository
                .atualizar(id, nome.trim(), emailNormalizado, perfil, situacao)
                .orElseThrow(() -> new NegocioException(UsuarioErrorsMessage.NAO_ENCONTRADO));

        auditoria.registrar("CONTA_ALTERADA", "%s (%s): perfil %s, situacao %s"
                .formatted(atualizado.nome(), atualizado.email(), perfil, situacao));

        return atualizado;
    }

    /**
     * Define uma senha nova para a conta. Quando nenhuma e informada, uma aleatoria e gerada e
     * devolvida, para que o administrador a repasse por um canal que ele escolha.
     *
     * @param id conta a alterar
     * @param senhaInformada senha escolhida, ou nulo para gerar
     * @return a conta afetada e a senha em texto, exibida uma unica vez
     */
    public SenhaRedefinida redefinirSenha(final Long id, final String senhaInformada) {
        final UsuarioResumo usuario = exigirExistente(id);
        final boolean gerada = senhaInformada == null || senhaInformada.isBlank();
        final String senha = gerada ? gerarSenha() : senhaInformada;

        usuarioRepository.trocarSenha(id, passwordEncoder.encode(senha))
                .orElseThrow(() -> new NegocioException(UsuarioErrorsMessage.NAO_ENCONTRADO));

        // A senha nao vai para o log: o registro guarda apenas que houve redefinicao.
        auditoria.registrar("SENHA_REDEFINIDA", "%s (%s)".formatted(usuario.nome(), usuario.email()));

        return new SenhaRedefinida(usuario, gerada ? senha : null);
    }

    /**
     * Remove a conta definitivamente.
     *
     * @param id conta a remover
     * @param emailDeQuemExclui e-mail do administrador que esta excluindo
     * @return a conta removida
     */
    public UsuarioResumo excluir(final Long id, final String emailDeQuemExclui) {
        final UsuarioResumo usuario = exigirExistente(id);

        impedirAcaoSobreSi(usuario, emailDeQuemExclui);

        if (usuario.perfil() == PerfilUsuario.MASTER && usuario.situacao() == SituacaoUsuario.ATIVO) {
            exigirOutroAdministrador();
        }

        usuarioRepository.excluir(id)
                .orElseThrow(() -> new NegocioException(UsuarioErrorsMessage.NAO_ENCONTRADO));

        auditoria.registrar("CONTA_EXCLUIDA", "%s (%s) como %s"
                .formatted(usuario.nome(), usuario.email(), usuario.perfil()));

        return usuario;
    }

    private UsuarioResumo exigirExistente(final Long id) {
        return usuarioRepository.buscarPorId(id)
                .orElseThrow(() -> new NegocioException(UsuarioErrorsMessage.NAO_ENCONTRADO));
    }

    private void impedirAcaoSobreSi(final UsuarioResumo alvo, final String emailDeQuemAge) {
        if (alvo.email().equalsIgnoreCase(emailDeQuemAge)) {
            throw new NegocioException(UsuarioErrorsMessage.ACAO_SOBRE_SI);
        }
    }

    private void exigirOutroAdministrador() {
        if (usuarioRepository.contarAdministradoresAtivos() <= 1) {
            throw new NegocioException(UsuarioErrorsMessage.ULTIMO_ADMINISTRADOR);
        }
    }

    private String gerarSenha() {
        final byte[] bytes = new byte[9];
        new SecureRandom().nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Resultado da redefinicao de senha.
     *
     * @param usuario conta afetada
     * @param senhaGerada senha em texto quando foi gerada pelo sistema, nulo quando foi informada
     */
    public record SenhaRedefinida(UsuarioResumo usuario, String senhaGerada) {
    }
}
