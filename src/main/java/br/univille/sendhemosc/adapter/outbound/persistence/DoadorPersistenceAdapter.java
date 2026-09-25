package br.univille.sendhemosc.adapter.outbound.persistence;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.DoadorEntity;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.DoadorJpaRepository;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.DoadorJpaRepository.CandidatoProjection;
import br.univille.sendhemosc.config.SendHemoscProperties;
import br.univille.sendhemosc.domain.dto.CandidatoConvocacao;
import br.univille.sendhemosc.domain.dto.DoadorCadastrado;
import br.univille.sendhemosc.domain.dto.FiltroDoador;
import br.univille.sendhemosc.domain.dto.NovoDoador;
import br.univille.sendhemosc.domain.enums.Sexo;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.exception.DoadorErrorsMessage;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementacao de persistencia da porta de doadores.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DoadorPersistenceAdapter implements IDoadorRepositoryPort {

    private final DoadorJpaRepository doadorRepository;
    private final SendHemoscProperties properties;

    @Override
    @Transactional(readOnly = true)
    public List<CandidatoConvocacao> buscarCandidatos(final Set<String> siglasTipoSanguineo, final LocalDate referencia) {
        return doadorRepository.buscarCandidatos(siglasTipoSanguineo, referencia.minusMonths(12),
                        inicioPrazoTeto(referencia)).stream()
                .map(this::paraCandidato)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CandidatoConvocacao> buscarPorFiltro(final FiltroDoador filtro, final LocalDate referencia) {
        final String sigla = filtro.tipoSanguineo() == null ? null : filtro.tipoSanguineo().getSigla();

        return doadorRepository.buscarPorFiltro(filtro.buscaComoPadrao(), sigla,
                        filtro.apenasComConsentimento(), referencia.minusMonths(12),
                        inicioPrazoTeto(referencia)).stream()
                .map(this::paraCandidato)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CandidatoConvocacao> buscarPorIdentificadores(final Set<Long> identificadores,
                                                              final LocalDate referencia) {
        if (identificadores.isEmpty()) {
            return List.of();
        }

        return doadorRepository.buscarPorIdentificadores(identificadores, referencia.minusMonths(12),
                        inicioPrazoTeto(referencia)).stream()
                .map(this::paraCandidato)
                .toList();
    }

    /**
     * Convocacoes sem resposta anteriores a este instante deixam de contar para o teto: e o prazo
     * que faz o limite se desfazer sozinho, sem depender de doacao nem de administrador.
     */
    private LocalDateTime inicioPrazoTeto(final LocalDate referencia) {
        return referencia.minusDays(properties.notificacao().prazoTetoDias()).atStartOfDay();
    }

    private CandidatoConvocacao paraCandidato(final CandidatoProjection projecao) {
        return new CandidatoConvocacao(
                projecao.getId(),
                projecao.getNome(),
                projecao.getEmail(),
                TipoSanguineo.doSigla(projecao.getTipoSanguineo()),
                projecao.getTokenDescadastro(),
                projecao.getAceitaContato(),
                Sexo.valueOf(projecao.getSexo()),
                projecao.getDataNascimento(),
                projecao.getPesoKg(),
                projecao.getUltimaDoacao(),
                projecao.getDoacoesJanela(),
                projecao.getConvocacoesSemResposta(),
                projecao.getUltimaConvocacao());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeComEmail(final String email) {
        return doadorRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    public Long salvar(final NovoDoador doador) {
        final LocalDateTime agora = LocalDateTime.now();

        final DoadorEntity entidade = doadorRepository.save(DoadorEntity.builder()
                .nome(doador.nome())
                .email(doador.email())
                .telefone(doador.telefone())
                .tipoSanguineo(doador.tipoSanguineo().getSigla())
                .sexo(doador.sexo())
                .dataNascimento(doador.dataNascimento())
                .pesoKg(doador.pesoKg())
                .ativo(true)
                .aceitaContato(doador.aceitaContato())
                .tokenDescadastro(UUID.randomUUID().toString())
                .criadoEm(agora)
                .atualizadoEm(agora)
                .build());

        return entidade.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public long contar() {
        return doadorRepository.count();
    }

    @Override
    @Transactional
    public boolean descadastrarPorToken(final String token) {
        return doadorRepository.descadastrarPorToken(token) > 0;
    }

    @Override
    @Transactional
    public boolean liberarContato(final Long id, final LocalDateTime quando) {
        return doadorRepository.liberarContato(id, quando) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DoadorCadastrado> buscarPorId(final Long id) {
        return doadorRepository.findById(id).map(this::paraCadastrado);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DoadorCadastrado> buscarPorToken(final String token) {
        return doadorRepository.findByTokenDescadastro(token).map(this::paraCadastrado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoadorCadastrado> listarInativos() {
        return doadorRepository.findByAtivoFalseOrderByNome().stream()
                .map(this::paraCadastrado)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeComEmailEmOutro(final String email, final Long id) {
        return doadorRepository.existsByEmailAndIdNot(email, id);
    }

    @Override
    @Transactional
    public void atualizar(final Long id, final NovoDoador dados) {
        final DoadorEntity entidade = doadorRepository.findById(id)
                .orElseThrow(() -> new NegocioException(DoadorErrorsMessage.NAO_ENCONTRADO));

        entidade.setNome(dados.nome());
        entidade.setEmail(dados.email());
        entidade.setTelefone(dados.telefone());
        entidade.setTipoSanguineo(dados.tipoSanguineo().getSigla());
        entidade.setSexo(dados.sexo());
        entidade.setDataNascimento(dados.dataNascimento());
        entidade.setPesoKg(dados.pesoKg());
        entidade.setAceitaContato(dados.aceitaContato());
        entidade.setAtualizadoEm(LocalDateTime.now());
        doadorRepository.save(entidade);
    }

    @Override
    @Transactional
    public boolean definirAtivo(final Long id, final boolean ativo) {
        return doadorRepository.findById(id)
                .map(entidade -> {
                    entidade.setAtivo(ativo);
                    entidade.setAtualizadoEm(LocalDateTime.now());
                    doadorRepository.save(entidade);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Doacoes, convocacoes e consentimentos saem junto pela chave estrangeira com ON DELETE
     * CASCADE, definida no schema: nao ficam registros orfaos com o id de quem pediu para sair.
     */
    @Override
    @Transactional
    public boolean excluir(final Long id) {
        if (!doadorRepository.existsById(id)) {
            return false;
        }

        doadorRepository.deleteById(id);
        return true;
    }

    private DoadorCadastrado paraCadastrado(final DoadorEntity entidade) {
        return new DoadorCadastrado(
                entidade.getId(),
                entidade.getNome(),
                entidade.getEmail(),
                entidade.getTelefone(),
                TipoSanguineo.doSigla(entidade.getTipoSanguineo()),
                entidade.getSexo(),
                entidade.getDataNascimento(),
                entidade.getPesoKg(),
                entidade.isAceitaContato(),
                entidade.isAtivo(),
                entidade.getCriadoEm(),
                entidade.getAtualizadoEm());
    }
}
