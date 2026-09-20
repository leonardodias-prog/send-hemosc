package br.univille.sendhemosc.usecase.doador;

import br.univille.sendhemosc.config.SendHemoscProperties;
import br.univille.sendhemosc.domain.dto.AptidaoDoador;
import br.univille.sendhemosc.domain.enums.Sexo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Nucleo de regra do sistema: decide se um doador esta apto a doar em uma data e, quando nao esta,
 * calcula a data estimada em que voltara a estar. Esta classe e deliberadamente pura e sem acesso a
 * banco, para que a equipe de hemoterapia possa revisar a regra em um unico arquivo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CalcularAptidaoUseCase {

    private final SendHemoscProperties properties;

    /**
     * Avalia a aptidao do doador.
     *
     * @param dados dados necessarios a avaliacao
     * @return resultado da avaliacao, com os motivos de inaptidao quando houver
     */
    public AptidaoDoador execute(final DadosAptidao dados) {
        final SendHemoscProperties.Aptidao regras = properties.aptidao();
        final List<String> motivos = new ArrayList<>();

        final int idade = Period.between(dados.dataNascimento(), dados.referencia()).getYears();
        if (idade < regras.idadeMinima() || idade > regras.idadeMaxima()) {
            motivos.add("doador.inapto.idade");
        }

        if (dados.pesoKg() != null && dados.pesoKg().compareTo(regras.pesoMinimoKg()) < 0) {
            motivos.add("doador.inapto.peso");
        }

        final int intervaloMinimo = intervaloMinimoDias(dados.sexo(), regras);
        final LocalDate proximaDataApta = dados.ultimaDoacao() == null
                ? dados.referencia()
                : dados.ultimaDoacao().plusDays(intervaloMinimo);

        if (proximaDataApta.isAfter(dados.referencia())) {
            motivos.add("doador.inapto.intervalo");
        }

        if (dados.doacoesUltimosDozeMeses() >= limiteAnual(dados.sexo(), regras)) {
            motivos.add("doador.inapto.limite-anual");
        }

        if (motivos.isEmpty()) {
            return AptidaoDoador.apto(proximaDataApta);
        }

        log.debug("[m=execute] Doador inapto na referencia {} por {}", dados.referencia(), motivos);
        return AptidaoDoador.inapto(proximaDataApta, motivos);
    }

    private int intervaloMinimoDias(final Sexo sexo, final SendHemoscProperties.Aptidao regras) {
        return sexo == Sexo.MASCULINO ? regras.intervaloDiasMasculino() : regras.intervaloDiasFeminino();
    }

    private int limiteAnual(final Sexo sexo, final SendHemoscProperties.Aptidao regras) {
        return sexo == Sexo.MASCULINO ? regras.maxDoacoesAnoMasculino() : regras.maxDoacoesAnoFeminino();
    }

    /**
     * Entrada da avaliacao de aptidao.
     *
     * @param sexo sexo biologico do doador
     * @param dataNascimento data de nascimento
     * @param pesoKg peso em quilos, pode ser nulo quando nao informado
     * @param ultimaDoacao data da ultima doacao, nula para quem nunca doou
     * @param doacoesUltimosDozeMeses total de doacoes na janela de doze meses
     * @param referencia data em que a aptidao esta sendo avaliada
     */
    public record DadosAptidao(
            Sexo sexo,
            LocalDate dataNascimento,
            BigDecimal pesoKg,
            LocalDate ultimaDoacao,
            long doacoesUltimosDozeMeses,
            LocalDate referencia) {
    }
}
