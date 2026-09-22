package br.univille.sendhemosc.domain.port.outbound;

import br.univille.sendhemosc.domain.dto.NovaDoacao;
import java.time.LocalDate;

/**
 * Porta de saida para o historico de doacoes.
 *
 * <p>Ate aqui a tabela so recebia linhas da massa ficticia. Como o calculo de aptidao le a
 * ultima doacao e conta as dos ultimos doze meses, sem estes registros toda pessoa permanecia
 * apta indefinidamente e voltava a ser convocada a cada rodada.</p>
 */
public interface IDoacaoRepositoryPort {

    /**
     * Grava uma doacao.
     *
     * @param doacao dados informados
     * @return identificador da doacao criada
     */
    Long registrar(NovaDoacao doacao);

    /**
     * Indica se ja existe doacao registrada para o doador naquela data.
     *
     * @param doadorId doador a verificar
     * @param data data da doacao
     * @return true quando ja ha registro no mesmo dia
     */
    boolean existeNoDia(Long doadorId, LocalDate data);
}
