package br.univille.sendhemosc.adapter.outbound.persistence;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.ConsentimentoEntity;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.ConsentimentoJpaRepository;
import br.univille.sendhemosc.domain.port.outbound.IConsentimentoPort;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grava o historico de consentimento, junto da versao do termo vigente no momento.
 */
@Slf4j
@Component
public class ConsentimentoPersistenceAdapter implements IConsentimentoPort {

    private final ConsentimentoJpaRepository consentimentoRepository;
    private final String versaoTermo;

    public ConsentimentoPersistenceAdapter(final ConsentimentoJpaRepository consentimentoRepository,
                                           @Value("${sendhemosc.termo.versao:1.0}") final String versaoTermo) {
        this.consentimentoRepository = consentimentoRepository;
        this.versaoTermo = versaoTermo;
    }

    @Override
    @Transactional
    public void registrar(final Long doadorId, final boolean aceito, final String origem) {
        consentimentoRepository.save(ConsentimentoEntity.builder()
                .doadorId(doadorId)
                .versaoTermo(versaoTermo)
                .aceito(aceito)
                .origem(origem)
                .registradoEm(LocalDateTime.now())
                .build());

        log.info("[m=registrar] Consentimento do doador {} registrado como {} pela origem {}, termo {}",
                doadorId, aceito ? "aceito" : "revogado", origem, versaoTermo);
    }
}
