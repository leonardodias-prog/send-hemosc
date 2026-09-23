package br.univille.sendhemosc.domain.dto;

import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import java.util.Set;

/**
 * O que o responsavel pode alterar no disparo automatico, pela tela.
 *
 * @param ativo se a rodada automatica pode acontecer
 * @param tiposSanguineos tipos que a rodada considera
 * @param limitePorRodada maximo de convocacoes em uma rodada, somados todos os tipos
 * @param horaInicio primeira hora cheia da janela, de 0 a 23
 * @param horaFim hora cheia em que a janela fecha, de 1 a 24
 * @param intervaloDias dias de calendario entre uma rodada e a seguinte
 */
public record AjusteDisparoAutomatico(
        boolean ativo,
        Set<TipoSanguineo> tiposSanguineos,
        int limitePorRodada,
        int horaInicio,
        int horaFim,
        int intervaloDias) {

    /**
     * Ajuste equivalente a configuracao vigente. Serve de ponto de partida do formulario e de
     * comparacao para saber se algo mudou.
     *
     * @param configuracao configuracao gravada
     * @return os campos editaveis dela
     */
    public static AjusteDisparoAutomatico de(final ConfiguracaoDisparoAutomatico configuracao) {
        return new AjusteDisparoAutomatico(configuracao.ativo(), configuracao.tiposSanguineos(),
                configuracao.limitePorRodada(), configuracao.horaInicio(), configuracao.horaFim(),
                configuracao.intervaloDias());
    }

    public boolean incluiTipo(final TipoSanguineo tipo) {
        return tiposSanguineos.contains(tipo);
    }
}
