package br.univille.sendhemosc.adapter.outbound.persistence;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.DoadorEntity;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.DoadorJpaRepository;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.DoadorJpaRepository.CandidatoProjection;
import br.univille.sendhemosc.config.SendHemoscProperties;
import br.univille.sendhemosc.domain.dto.CandidatoConvocacao;
import br.univille.sendhemosc.domain.dto.FiltroDoador;
import br.univille.sendhemosc.domain.dto.NovoDoador;
import br.univille.sendhemosc.domain.enums.Sexo;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
}
