package br.univille.sendhemosc.domain.port.outbound;

import br.univille.sendhemosc.domain.enums.NivelEstoque;
import br.univille.sendhemosc.domain.enums.OrigemNotificacao;
import br.univille.sendhemosc.domain.enums.StatusNotificacao;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import java.time.LocalDate;
import java.util.List;

/**
 * Porta de saida para o historico de convocacoes.
 */
public interface INotificacaoRepositoryPort {

    /**
     * Registra uma convocacao.
     *
     * @param doadorId doador convocado
     * @param tipoSanguineo tipo que motivou a convocacao
     * @param nivel nivel do estoque no momento do disparo
     * @param origem como a convocacao foi disparada
     * @param status resultado do envio
     * @param tentativa numero da tentativa
     * @param erro mensagem de erro, quando houver
     */
    void registrar(Long doadorId, TipoSanguineo tipoSanguineo, NivelEstoque nivel,
                   OrigemNotificacao origem, StatusNotificacao status, int tentativa, String erro);

    /**
     * Identifica doadores que receberam convocacao ha mais tempo que o intervalo de reenvio
     * e que ainda nao registraram comparecimento.
     *
     * @param tipoSanguineo tipo sanguineo alvo
     * @param anteriorA data limite: convocacoes enviadas antes dela entram no reenvio
     * @return identificadores de doadores a reconvocar
     */
    List<Long> buscarPendentesDeReenvio(TipoSanguineo tipoSanguineo, LocalDate anteriorA);

    /**
     * Marca como atendidas as convocacoes que o doador ainda tinha em aberto.
     *
     * <p>E o que liga a doacao a convocacao que a motivou. Sem isso a pessoa que atendeu ao
     * chamado continua constando como pendente, e nao ha como medir se a convocacao funcionou.
     * Fecha todas as pendentes, e nao apenas a ultima: qualquer uma delas pode ter sido o
     * motivo da visita, e deixar as demais abertas manteria um pendente que nunca se resolve.</p>
     *
     * @param doadorId quem doou
     * @param dataDoacao data em que a doacao aconteceu
     * @return quantas convocacoes passaram a constar como atendidas
     */
    int marcarComparecimento(Long doadorId, LocalDate dataDoacao);

    /**
     * Numero da proxima tentativa de convocacao para o doador no tipo informado.
     *
     * @param doadorId identificador do doador
     * @param tipoSanguineo tipo sanguineo alvo
     * @return numero da proxima tentativa
     */
    int proximaTentativa(Long doadorId, TipoSanguineo tipoSanguineo);
}
