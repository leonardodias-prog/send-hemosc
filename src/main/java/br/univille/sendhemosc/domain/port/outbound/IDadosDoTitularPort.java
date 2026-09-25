package br.univille.sendhemosc.domain.port.outbound;

import br.univille.sendhemosc.domain.dto.DadosDoTitular;
import java.util.List;

/**
 * Porta de saida para reunir o historico de um doador guardado fora do cadastro: consentimentos,
 * doacoes e convocacoes. Alimenta a exportacao dos dados do titular.
 */
public interface IDadosDoTitularPort {

    /**
     * Historico de consentimento, do mais antigo ao mais recente.
     *
     * @param doadorId doador
     * @return manifestacoes registradas
     */
    List<DadosDoTitular.Consentimento> consentimentos(Long doadorId);

    /**
     * Doacoes registradas, da mais antiga a mais recente.
     *
     * @param doadorId doador
     * @return doacoes
     */
    List<DadosDoTitular.Doacao> doacoes(Long doadorId);

    /**
     * Convocacoes registradas, da mais antiga a mais recente.
     *
     * @param doadorId doador
     * @return convocacoes
     */
    List<DadosDoTitular.Convocacao> convocacoes(Long doadorId);
}
