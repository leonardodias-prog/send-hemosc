package br.univille.sendhemosc.adapter.outbound.persistence;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.DisparoAutomaticoEntity;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.DisparoAutomaticoJpaRepository;
import br.univille.sendhemosc.domain.dto.AjusteDisparoAutomatico;
import br.univille.sendhemosc.domain.dto.ConfiguracaoDisparoAutomatico;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.port.outbound.IDisparoAutomaticoRepositoryPort;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementacao de persistencia da configuracao do disparo automatico.
 */
@Component
@RequiredArgsConstructor
public class DisparoAutomaticoPersistenceAdapter implements IDisparoAutomaticoRepositoryPort {

    /** A migration cria esta linha e a restricao do banco impede qualquer outra. */
    private static final Long LINHA = 1L;

    private static final String SEPARADOR = ",";

    private final DisparoAutomaticoJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public ConfiguracaoDisparoAutomatico buscar() {
        final DisparoAutomaticoEntity entidade = linha();

        return new ConfiguracaoDisparoAutomatico(
                entidade.isAtivo(),
                paraTipos(entidade.getTiposSanguineos()),
                entidade.getLimitePorRodada(),
                entidade.getHoraInicio(),
                entidade.getHoraFim(),
                entidade.getIntervaloDias(),
                entidade.getUltimaRodadaEm(),
                entidade.getUltimaRodadaResumo(),
                entidade.getAtualizadoPor(),
                entidade.getAtualizadoEm());
    }

    @Override
    @Transactional
    public void salvar(final AjusteDisparoAutomatico ajuste, final String autor, final LocalDateTime quando) {
        final DisparoAutomaticoEntity entidade = linha();

        entidade.setAtivo(ajuste.ativo());
        entidade.setTiposSanguineos(paraTexto(ajuste.tiposSanguineos()));
        entidade.setLimitePorRodada(ajuste.limitePorRodada());
        entidade.setHoraInicio(ajuste.horaInicio());
        entidade.setHoraFim(ajuste.horaFim());
        entidade.setIntervaloDias(ajuste.intervaloDias());
        entidade.setAtualizadoPor(autor);
        entidade.setAtualizadoEm(quando);
    }

    @Override
    @Transactional
    public boolean assumirRodada(final LocalDateTime anterior, final LocalDateTime inicio) {
        final int assumidas = anterior == null
                ? repository.assumirPrimeiraRodada(LINHA, inicio)
                : repository.assumirRodada(LINHA, anterior, inicio);

        return assumidas == 1;
    }

    @Override
    @Transactional
    public void registrarResumoDaRodada(final String resumo) {
        repository.registrarResumo(LINHA, resumo);
    }

    private DisparoAutomaticoEntity linha() {
        return repository.findById(LINHA)
                .orElseThrow(() -> new IllegalStateException(
                        "Configuracao do disparo automatico ausente: a migration V5 deveria te-la criado"));
    }

    private Set<TipoSanguineo> paraTipos(final String texto) {
        final Set<TipoSanguineo> tipos = EnumSet.noneOf(TipoSanguineo.class);

        Arrays.stream(texto.split(SEPARADOR))
                .map(String::trim)
                .filter(sigla -> !sigla.isEmpty())
                .map(TipoSanguineo::doSigla)
                .forEach(tipos::add);

        return tipos;
    }

    private String paraTexto(final Set<TipoSanguineo> tipos) {
        // EnumSet ordena pelo enum, entao o texto gravado nao depende da ordem em que chegou. Nao
        // usa EnumSet.copyOf: ele recusa colecao vazia, e desligar sem tipo marcado e valido.
        final Set<TipoSanguineo> ordenados = EnumSet.noneOf(TipoSanguineo.class);
        ordenados.addAll(tipos);

        return ordenados.stream()
                .map(TipoSanguineo::getSigla)
                .collect(Collectors.joining(SEPARADOR));
    }
}
