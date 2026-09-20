package br.univille.sendhemosc.config;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.DoacaoEntity;
import br.univille.sendhemosc.adapter.outbound.persistence.entity.DoadorEntity;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.DoacaoJpaRepository;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.DoadorJpaRepository;
import br.univille.sendhemosc.domain.enums.Sexo;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gera a massa de dados ficticios usada na demonstracao. Os e-mails apontam para o dominio
 * reservado example.org, de modo que nenhum endereco real possa ser atingido mesmo por engano.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sendhemosc.seed.habilitado", havingValue = "true")
public class SeedDadosFicticiosRunner implements ApplicationRunner {

    private static final String DOMINIO_SEGURO = "@example.org";
    private static final BigDecimal PESO_MINIMO = BigDecimal.valueOf(48);
    private static final BigDecimal PESO_VARIACAO = BigDecimal.valueOf(45);

    private final DoadorJpaRepository doadorRepository;
    private final DoacaoJpaRepository doacaoRepository;
    private final int quantidadeDoadores;
    private final Faker faker = new Faker(Locale.of("pt", "BR"));

    public SeedDadosFicticiosRunner(final DoadorJpaRepository doadorRepository,
                                    final DoacaoJpaRepository doacaoRepository,
                                    @Value("${sendhemosc.seed.quantidade-doadores:120}") final int quantidadeDoadores) {
        this.doadorRepository = doadorRepository;
        this.doacaoRepository = doacaoRepository;
        this.quantidadeDoadores = quantidadeDoadores;
    }

    @Override
    @Transactional
    public void run(final ApplicationArguments args) {
        if (doadorRepository.count() > 0) {
            log.info("[m=run] Base ja possui doadores, seed ignorado");
            return;
        }

        final TipoSanguineo[] tipos = TipoSanguineo.values();
        final List<DoadorEntity> doadores = new ArrayList<>(quantidadeDoadores);

        for (int i = 0; i < quantidadeDoadores; i++) {
            doadores.add(construirDoador(tipos[ThreadLocalRandom.current().nextInt(tipos.length)], i));
        }

        doadorRepository.saveAll(doadores);
        doadores.forEach(this::construirHistoricoDoacoes);

        log.info("[m=run] Seed concluido: {} doadores ficticios criados", doadores.size());
        log.warn("[m=run] ATENCAO: base populada com DADOS FICTICIOS, apenas para demonstracao");
    }

    private DoadorEntity construirDoador(final TipoSanguineo tipo, final int indice) {
        final LocalDateTime agora = LocalDateTime.now();
        final int idade = ThreadLocalRandom.current().nextInt(16, 70);

        return DoadorEntity.builder()
                .nome(faker.name().fullName())
                .email("doador" + indice + DOMINIO_SEGURO)
                .telefone(faker.phoneNumber().cellPhone())
                .tipoSanguineo(tipo.getSigla())
                .sexo(ThreadLocalRandom.current().nextBoolean() ? Sexo.MASCULINO : Sexo.FEMININO)
                .dataNascimento(LocalDate.now().minusYears(idade).minusDays(ThreadLocalRandom.current().nextInt(365)))
                .pesoKg(PESO_MINIMO.add(PESO_VARIACAO.multiply(BigDecimal.valueOf(Math.random())))
                        .setScale(2, RoundingMode.HALF_UP))
                .ativo(true)
                .aceitaContato(ThreadLocalRandom.current().nextInt(100) < 85)
                .tokenDescadastro(UUID.randomUUID().toString())
                .criadoEm(agora)
                .atualizadoEm(agora)
                .build();
    }

    private void construirHistoricoDoacoes(final DoadorEntity doador) {
        final int totalDoacoes = ThreadLocalRandom.current().nextInt(0, 4);
        final List<DoacaoEntity> doacoes = new ArrayList<>(totalDoacoes);

        for (int i = 0; i < totalDoacoes; i++) {
            doacoes.add(DoacaoEntity.builder()
                    .doadorId(doador.getId())
                    .dataDoacao(LocalDate.now().minusDays(ThreadLocalRandom.current().nextInt(20, 900)))
                    .localColeta("Agencia Transfusional - Joinville")
                    .criadoEm(LocalDateTime.now())
                    .build());
        }

        doacaoRepository.saveAll(doacoes);
    }
}
