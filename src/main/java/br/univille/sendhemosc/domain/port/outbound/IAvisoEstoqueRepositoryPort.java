package br.univille.sendhemosc.domain.port.outbound;

import br.univille.sendhemosc.domain.dto.ConfiguracaoAvisoEstoque;
import br.univille.sendhemosc.domain.dto.EnvioAvisoEstoque;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Porta de saida para a configuracao e o registro do aviso interno de estoque.
 */
public interface IAvisoEstoqueRepositoryPort {

    ConfiguracaoAvisoEstoque buscar();

    /**
     * Grava a hora do relatorio diario.
     *
     * @param horaRelatorio hora a partir da qual o relatorio sai
     * @param autor quem alterou
     * @param quando momento da alteracao
     */
    void salvar(int horaRelatorio, String autor, LocalDateTime quando);

    /**
     * Marca o relatorio do dia como enviado, se ainda nao estava.
     *
     * @param dia dia do relatorio
     * @return true se esta chamada assumiu o relatorio; false se o do dia ja tinha saido
     */
    boolean assumirRelatorioDoDia(LocalDate dia);

    void registrarEnvio(EnvioAvisoEstoque envio);

    /**
     * Lista os avisos mais recentes, do mais novo para o mais antigo.
     *
     * @param limite quantidade maxima de registros
     * @return avisos enviados
     */
    List<EnvioAvisoEstoque> ultimosEnvios(int limite);
}
