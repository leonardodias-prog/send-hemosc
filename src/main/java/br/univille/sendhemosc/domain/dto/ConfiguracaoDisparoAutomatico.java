package br.univille.sendhemosc.domain.dto;

import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

/**
 * Como o disparo automatico esta configurado, e quando rodou pela ultima vez.
 *
 * @param ativo se a rodada automatica pode acontecer
 * @param tiposSanguineos tipos que a rodada considera; os demais ficam so com a convocacao manual
 * @param limitePorRodada maximo de convocacoes enviadas em uma rodada, somados todos os tipos
 * @param horaInicio primeira hora cheia da janela, de 0 a 23
 * @param horaFim hora cheia em que a janela fecha, de 1 a 24, sem incluir a propria hora
 * @param intervaloDias dias de calendario entre uma rodada e a seguinte
 * @param ultimaRodadaEm inicio da ultima rodada, nulo se nunca houve
 * @param ultimaRodadaResumo o que a ultima rodada fez, em uma frase
 * @param atualizadoPor quem alterou a configuracao pela ultima vez
 * @param atualizadoEm quando a configuracao foi alterada pela ultima vez
 */
public record ConfiguracaoDisparoAutomatico(
        boolean ativo,
        Set<TipoSanguineo> tiposSanguineos,
        int limitePorRodada,
        int horaInicio,
        int horaFim,
        int intervaloDias,
        LocalDateTime ultimaRodadaEm,
        String ultimaRodadaResumo,
        String atualizadoPor,
        LocalDateTime atualizadoEm) {

    /**
     * Fuso em que a janela e interpretada. O servidor publicado roda em UTC: sem fixar o fuso,
     * "das 8h as 18h" viraria das 5h as 15h no relogio de quem configurou.
     */
    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    /**
     * Se a rodada deve acontecer agora: ligada, dentro da janela e com o intervalo cumprido.
     *
     * <p>O intervalo conta em dias de calendario, e nao em horas. Assim a rodada diaria nao
     * escorrega no horario de um dia para o outro: acontece na primeira verificacao dentro da
     * janela do dia devido, seja qual for a hora em que a anterior rodou.</p>
     *
     * @param agora momento da verificacao, no {@link #FUSO}
     * @return true quando a rodada e devida
     */
    public boolean rodadaDevida(final LocalDateTime agora) {
        return ativo
                && dentroDaJanela(agora)
                && !primeiroDiaPermitido(agora.toLocalDate()).isAfter(agora.toLocalDate());
    }

    /**
     * Proximo momento em que a rodada pode acontecer, para mostrar na tela.
     *
     * @param agora momento da consulta, no {@link #FUSO}
     * @return nulo quando desligado; o proprio agora quando a rodada ja e devida
     */
    public LocalDateTime proximaRodada(final LocalDateTime agora) {
        if (!ativo) {
            return null;
        }

        final LocalDate hoje = agora.toLocalDate();
        final LocalDate dia = primeiroDiaPermitido(hoje);

        if (dia.isAfter(hoje)) {
            return dia.atTime(horaInicio, 0);
        }
        if (agora.getHour() < horaInicio) {
            return hoje.atTime(horaInicio, 0);
        }
        if (agora.getHour() < horaFim) {
            return agora;
        }

        return hoje.plusDays(1).atTime(horaInicio, 0);
    }

    private boolean dentroDaJanela(final LocalDateTime agora) {
        return agora.getHour() >= horaInicio && agora.getHour() < horaFim;
    }

    private LocalDate primeiroDiaPermitido(final LocalDate hoje) {
        return ultimaRodadaEm == null ? hoje : ultimaRodadaEm.toLocalDate().plusDays(intervaloDias);
    }
}
