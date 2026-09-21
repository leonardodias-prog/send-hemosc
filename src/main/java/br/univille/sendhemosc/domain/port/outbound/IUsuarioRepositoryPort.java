package br.univille.sendhemosc.domain.port.outbound;

import br.univille.sendhemosc.domain.dto.UsuarioAutenticavel;
import br.univille.sendhemosc.domain.dto.UsuarioResumo;
import br.univille.sendhemosc.domain.enums.PerfilUsuario;
import br.univille.sendhemosc.domain.enums.SituacaoUsuario;
import java.util.List;
import java.util.Optional;

/**
 * Porta de saida para as contas de usuario.
 */
public interface IUsuarioRepositoryPort {

    /**
     * Busca uma conta pelo e-mail, para autenticacao.
     *
     * @param email endereco informado no login
     * @return a conta, se existir
     */
    Optional<UsuarioAutenticavel> buscarPorEmail(String email);

    Optional<UsuarioResumo> buscarPorId(Long id);

    boolean existeComEmail(String email);

    /**
     * Indica se o e-mail pertence a outra conta que nao a informada. Usado na edicao, onde
     * manter o proprio e-mail precisa ser permitido.
     *
     * @param email endereco a verificar
     * @param exetoUsuarioId conta que nao deve ser considerada
     * @return true se outra conta ja usa o e-mail
     */
    boolean existeComEmailDeOutro(String email, Long exetoUsuarioId);

    /**
     * Cria uma conta. A senha ja deve chegar convertida em hash.
     *
     * @param nome nome completo
     * @param email endereco de e-mail
     * @param senhaHash hash BCrypt da senha
     * @param perfil perfil pretendido
     * @param situacao situacao inicial da conta
     * @param tokenAprovacao token do link de aprovacao, nulo quando nao ha aprovacao pendente
     * @return identificador da conta criada
     */
    Long criar(String nome, String email, String senhaHash, PerfilUsuario perfil,
               SituacaoUsuario situacao, String tokenAprovacao);

    /**
     * Altera os dados cadastrais de uma conta. Nao mexe na senha.
     *
     * @param id conta a alterar
     * @param nome novo nome
     * @param email novo e-mail
     * @param perfil novo perfil
     * @param situacao nova situacao
     * @return a conta atualizada, vazio quando nao existe
     */
    Optional<UsuarioResumo> atualizar(Long id, String nome, String email,
                                      PerfilUsuario perfil, SituacaoUsuario situacao);

    /**
     * Grava um novo hash de senha.
     *
     * @param id conta a alterar
     * @param senhaHash novo hash BCrypt
     * @return a conta atualizada, vazio quando nao existe
     */
    Optional<UsuarioResumo> trocarSenha(Long id, String senhaHash);

    /**
     * Remove a conta definitivamente.
     *
     * @param id conta a remover
     * @return a conta removida, vazio quando nao existe
     */
    Optional<UsuarioResumo> excluir(Long id);

    /**
     * Resolve uma solicitacao pendente a partir do token do link enviado ao administrador.
     *
     * @param token token recebido no e-mail
     * @param aprovado true para liberar a conta, false para recusar
     * @param aprovadorId identificador de quem decidiu, nulo quando a decisao veio pelo link
     * @return o usuario afetado, vazio quando o token nao corresponde a solicitacao pendente
     */
    Optional<UsuarioResumo> resolverAprovacaoPorToken(String token, boolean aprovado, Long aprovadorId);

    /**
     * Resolve uma solicitacao pendente pela tela de gerenciamento.
     *
     * @param usuarioId conta a decidir
     * @param aprovado true para liberar, false para recusar
     * @param aprovadorId identificador de quem decidiu
     * @return o usuario afetado, vazio quando nao havia solicitacao pendente
     */
    Optional<UsuarioResumo> resolverAprovacao(Long usuarioId, boolean aprovado, Long aprovadorId);

    List<UsuarioResumo> listarTodos();

    List<UsuarioResumo> listarPorSituacao(SituacaoUsuario situacao);

    /**
     * Enderecos de e-mail dos administradores ativos, destinatarios dos pedidos de aprovacao.
     *
     * @return lista de e-mails
     */
    List<String> emailsDosAdministradores();

    /**
     * Quantidade de administradores ativos. Serve para impedir que o ultimo seja removido ou
     * rebaixado, o que deixaria o sistema sem ninguem capaz de aprovar cadastros.
     *
     * @return total de contas de administrador em situacao ativa
     */
    long contarAdministradoresAtivos();

    void registrarAcesso(String email);

    boolean existeAlgumComPerfil(PerfilUsuario perfil);
}
