package br.univille.sendhemosc.adapter.outbound.persistence;

import br.univille.sendhemosc.adapter.outbound.persistence.repository.DoadorJpaRepository;
import br.univille.sendhemosc.domain.dto.CandidatoConvocacao;
import br.univille.sendhemosc.domain.enums.Sexo;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
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

    @Override
    @Transactional(readOnly = true)
    public List<CandidatoConvocacao> buscarCandidatos(final Set<String> siglasTipoSanguineo, final LocalDate referencia) {
        final LocalDate inicioJanela = referencia.minusMonths(12);

        return doadorRepository.buscarCandidatos(siglasTipoSanguineo, inicioJanela).stream()
                .map(projecao -> new CandidatoConvocacao(
                        projecao.getId(),
                        projecao.getNome(),
                        projecao.getEmail(),
                        TipoSanguineo.doSigla(projecao.getTipoSanguineo()),
                        projecao.getTokenDescadastro(),
                        Sexo.valueOf(projecao.getSexo()),
                        projecao.getDataNascimento(),
                        projecao.getPesoKg(),
                        projecao.getUltimaDoacao(),
                        projecao.getDoacoesJanela()))
                .toList();
    }

    @Override
    @Transactional
    public boolean descadastrarPorToken(final String token) {
        return doadorRepository.descadastrarPorToken(token) > 0;
    }
}
