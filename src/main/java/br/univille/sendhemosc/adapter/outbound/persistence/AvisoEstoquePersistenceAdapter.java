package br.univille.sendhemosc.adapter.outbound.persistence;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.AvisoEstoqueEntity;
import br.univille.sendhemosc.adapter.outbound.persistence.entity.AvisoEstoqueEnvioEntity;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.AvisoEstoqueEnvioJpaRepository;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.AvisoEstoqueJpaRepository;
import br.univille.sendhemosc.domain.dto.ConfiguracaoAvisoEstoque;
import br.univille.sendhemosc.domain.dto.EnvioAvisoEstoque;
import br.univille.sendhemosc.domain.port.outbound.IAvisoEstoqueRepositoryPort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementacao de persistencia do aviso interno de estoque.
 */
@Component
@RequiredArgsConstructor
public class AvisoEstoquePersistenceAdapter implements IAvisoEstoqueRepositoryPort {

    /** A migration cria esta linha e a restricao do banco impede qualquer outra. */
    private static final Long LINHA = 1L;

    private final AvisoEstoqueJpaRepository repository;
    private final AvisoEstoqueEnvioJpaRepository envioRepository;

    @Override
    @Transactional(readOnly = true)
    public ConfiguracaoAvisoEstoque buscar() {
        final AvisoEstoqueEntity entidade = linha();

        return new ConfiguracaoAvisoEstoque(
                entidade.getHoraRelatorio(),
                entidade.getUltimoRelatorioEm(),
                entidade.getAtualizadoPor(),
                entidade.getAtualizadoEm());
    }

    @Override
    @Transactional
    public void salvar(final int horaRelatorio, final String autor, final LocalDateTime quando) {
        final AvisoEstoqueEntity entidade = linha();

        entidade.setHoraRelatorio(horaRelatorio);
        entidade.setAtualizadoPor(autor);
        entidade.setAtualizadoEm(quando);
    }

    @Override
    @Transactional
    public boolean assumirRelatorioDoDia(final LocalDate dia) {
        return repository.assumirRelatorioDoDia(LINHA, dia) == 1;
    }

    @Override
    @Transactional
    public void registrarEnvio(final EnvioAvisoEstoque envio) {
        envioRepository.save(AvisoEstoqueEnvioEntity.builder()
                .tipo(envio.tipo())
                .assunto(envio.assunto())
                .destinatarios(envio.destinatarios())
                .falhas(envio.falhas())
                .enviadoEm(envio.enviadoEm())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnvioAvisoEstoque> ultimosEnvios(final int limite) {
        return envioRepository.findAllByOrderByEnviadoEmDescIdDesc(PageRequest.of(0, limite)).stream()
                .map(entidade -> new EnvioAvisoEstoque(entidade.getTipo(), entidade.getAssunto(),
                        entidade.getDestinatarios(), entidade.getFalhas(), entidade.getEnviadoEm()))
                .toList();
    }

    private AvisoEstoqueEntity linha() {
        return repository.findById(LINHA)
                .orElseThrow(() -> new IllegalStateException(
                        "Configuracao do aviso de estoque ausente: a migration V10 deveria te-la criado"));
    }
}
