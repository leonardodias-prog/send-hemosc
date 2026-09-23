package br.univille.sendhemosc.domain.port.outbound;

import br.univille.sendhemosc.domain.dto.AjusteDisparoAutomatico;
import br.univille.sendhemosc.domain.dto.ConfiguracaoDisparoAutomatico;
import java.time.LocalDateTime;

/**
 * Porta de saida para a configuracao do disparo automatico.
 */
public interface IDisparoAutomaticoRepositoryPort {

    ConfiguracaoDisparoAutomatico buscar();

    /**
     * Grava o que o responsavel alterou. Nao mexe no registro da ultima rodada.
     *
     * @param ajuste campos editaveis
     * @param autor quem alterou
     * @param quando momento da alteracao
     */
    void salvar(AjusteDisparoAutomatico ajuste, String autor, LocalDateTime quando);

    /**
     * Assume a rodada, marcando o seu inicio, somente se nenhuma outra verificacao fez isso depois
     * da leitura. Durante uma publicacao duas instancias chegam a rodar juntas por alguns
     * instantes; sem esta troca condicional, as duas poderiam disparar a mesma rodada.
     *
     * @param anterior ultima rodada que a verificacao leu, nula se nunca houve
     * @param inicio inicio da nova rodada
     * @return true se esta verificacao ficou com a rodada
     */
    boolean assumirRodada(LocalDateTime anterior, LocalDateTime inicio);

    void registrarResumoDaRodada(String resumo);
}
