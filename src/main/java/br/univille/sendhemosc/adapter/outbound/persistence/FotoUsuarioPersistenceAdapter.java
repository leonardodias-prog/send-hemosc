package br.univille.sendhemosc.adapter.outbound.persistence;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.FotoUsuarioEntity;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.FotoUsuarioJpaRepository;
import br.univille.sendhemosc.domain.dto.FotoDoUsuario;
import br.univille.sendhemosc.domain.port.outbound.IFotoUsuarioRepositoryPort;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementacao de persistencia da porta de fotos de perfil.
 */
@Component
@RequiredArgsConstructor
public class FotoUsuarioPersistenceAdapter implements IFotoUsuarioRepositoryPort {

    private final FotoUsuarioJpaRepository fotoRepository;

    @Override
    @Transactional
    public void salvar(final Long usuarioId, final String tipoConteudo, final byte[] conteudo,
                       final LocalDateTime quando) {
        fotoRepository.save(FotoUsuarioEntity.builder()
                .usuarioId(usuarioId)
                .tipoConteudo(tipoConteudo)
                .conteudo(conteudo)
                .atualizadoEm(quando)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FotoDoUsuario> buscar(final Long usuarioId) {
        return fotoRepository.findById(usuarioId)
                .map(entidade -> new FotoDoUsuario(entidade.getTipoConteudo(), entidade.getConteudo(),
                        entidade.getAtualizadoEm()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LocalDateTime> momentoDaFoto(final Long usuarioId) {
        return fotoRepository.momentoDaFoto(usuarioId);
    }

    @Override
    @Transactional
    public boolean remover(final Long usuarioId) {
        return fotoRepository.apagarDaConta(usuarioId) > 0;
    }
}
