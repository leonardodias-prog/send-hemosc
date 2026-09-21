package br.univille.sendhemosc.adapter.outbound.persistence;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.UsuarioEntity;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.UsuarioJpaRepository;
import br.univille.sendhemosc.domain.dto.UsuarioAutenticavel;
import br.univille.sendhemosc.domain.dto.UsuarioResumo;
import br.univille.sendhemosc.domain.enums.PerfilUsuario;
import br.univille.sendhemosc.domain.enums.SituacaoUsuario;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementacao de persistencia da porta de usuarios.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsuarioPersistenceAdapter implements IUsuarioRepositoryPort {

    private final UsuarioJpaRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<UsuarioAutenticavel> buscarPorEmail(final String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .map(entidade -> new UsuarioAutenticavel(
                        entidade.getId(), entidade.getNome(), entidade.getEmail(),
                        entidade.getSenhaHash(), entidade.getPerfil(), entidade.getSituacao()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UsuarioResumo> buscarPorId(final Long id) {
        return usuarioRepository.findById(id).map(this::paraResumo);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeComEmail(final String email) {
        return usuarioRepository.existsByEmailIgnoreCase(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeComEmailDeOutro(final String email, final Long exetoUsuarioId) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .filter(entidade -> !entidade.getId().equals(exetoUsuarioId))
                .isPresent();
    }

    @Override
    @Transactional
    public Optional<UsuarioResumo> atualizar(final Long id, final String nome, final String email,
                                             final PerfilUsuario perfil, final SituacaoUsuario situacao) {
        return usuarioRepository.findById(id).map(entidade -> {
            entidade.setNome(nome);
            entidade.setEmail(email);
            entidade.setPerfil(perfil);
            entidade.setSituacao(situacao);
            entidade.setAtualizadoEm(LocalDateTime.now());

            // Conta que deixa de estar pendente nao precisa mais do token do link de aprovacao.
            if (situacao != SituacaoUsuario.PENDENTE) {
                entidade.setTokenAprovacao(null);
            }

            return paraResumo(usuarioRepository.save(entidade));
        });
    }

    @Override
    @Transactional
    public Optional<UsuarioResumo> trocarSenha(final Long id, final String senhaHash) {
        return usuarioRepository.findById(id).map(entidade -> {
            entidade.setSenhaHash(senhaHash);
            entidade.setAtualizadoEm(LocalDateTime.now());

            return paraResumo(usuarioRepository.save(entidade));
        });
    }

    @Override
    @Transactional
    public Optional<UsuarioResumo> excluir(final Long id) {
        return usuarioRepository.findById(id).map(entidade -> {
            final UsuarioResumo removido = paraResumo(entidade);
            usuarioRepository.delete(entidade);

            return removido;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public long contarAdministradoresAtivos() {
        return usuarioRepository.countByPerfilAndSituacao(PerfilUsuario.MASTER, SituacaoUsuario.ATIVO);
    }

    @Override
    @Transactional
    public Long criar(final String nome, final String email, final String senhaHash,
                      final PerfilUsuario perfil, final SituacaoUsuario situacao, final String tokenAprovacao) {
        final LocalDateTime agora = LocalDateTime.now();

        return usuarioRepository.save(UsuarioEntity.builder()
                .nome(nome)
                .email(email)
                .senhaHash(senhaHash)
                .perfil(perfil)
                .situacao(situacao)
                .tokenAprovacao(tokenAprovacao)
                .criadoEm(agora)
                .atualizadoEm(agora)
                .build()).getId();
    }

    @Override
    @Transactional
    public Optional<UsuarioResumo> resolverAprovacaoPorToken(final String token, final boolean aprovado,
                                                             final Long aprovadorId) {
        return usuarioRepository.findByTokenAprovacao(token)
                .filter(entidade -> entidade.getSituacao() == SituacaoUsuario.PENDENTE)
                .map(entidade -> aplicarDecisao(entidade, aprovado, aprovadorId));
    }

    @Override
    @Transactional
    public Optional<UsuarioResumo> resolverAprovacao(final Long usuarioId, final boolean aprovado,
                                                     final Long aprovadorId) {
        return usuarioRepository.findById(usuarioId)
                .filter(entidade -> entidade.getSituacao() == SituacaoUsuario.PENDENTE)
                .map(entidade -> aplicarDecisao(entidade, aprovado, aprovadorId));
    }

    private UsuarioResumo aplicarDecisao(final UsuarioEntity entidade, final boolean aprovado,
                                         final Long aprovadorId) {
        final LocalDateTime agora = LocalDateTime.now();

        entidade.setSituacao(aprovado ? SituacaoUsuario.ATIVO : SituacaoUsuario.RECUSADO);
        entidade.setAprovadoPor(aprovadorId);
        entidade.setAprovadoEm(agora);
        entidade.setAtualizadoEm(agora);
        // O token e de uso unico: some assim que a decisao e tomada.
        entidade.setTokenAprovacao(null);

        return paraResumo(usuarioRepository.save(entidade));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResumo> listarTodos() {
        return usuarioRepository.findAllByOrderByCriadoEmDesc().stream().map(this::paraResumo).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResumo> listarPorSituacao(final SituacaoUsuario situacao) {
        return usuarioRepository.findBySituacaoOrderByCriadoEmDesc(situacao).stream()
                .map(this::paraResumo).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> emailsDosAdministradores() {
        return usuarioRepository.findByPerfilAndSituacao(PerfilUsuario.MASTER, SituacaoUsuario.ATIVO)
                .stream().map(UsuarioEntity::getEmail).toList();
    }

    @Override
    @Transactional
    public void registrarAcesso(final String email) {
        usuarioRepository.findByEmailIgnoreCase(email).ifPresent(entidade -> {
            entidade.setUltimoAcessoEm(LocalDateTime.now());
            usuarioRepository.save(entidade);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeAlgumComPerfil(final PerfilUsuario perfil) {
        return usuarioRepository.existsByPerfil(perfil);
    }

    private UsuarioResumo paraResumo(final UsuarioEntity entidade) {
        return new UsuarioResumo(entidade.getId(), entidade.getNome(), entidade.getEmail(),
                entidade.getPerfil(), entidade.getSituacao(), entidade.getCriadoEm(),
                entidade.getUltimoAcessoEm());
    }
}
