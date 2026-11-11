package br.univille.sendhemosc.usecase.estoque;

import br.univille.sendhemosc.domain.dto.ConfiguracaoAvisoEstoque;
import br.univille.sendhemosc.domain.dto.ConfiguracaoDisparoAutomatico;
import br.univille.sendhemosc.domain.dto.MinimosDoTipo;
import br.univille.sendhemosc.domain.exception.EstoqueErrorsMessage;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import br.univille.sendhemosc.domain.port.outbound.IAvisoEstoqueRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.IEstoqueRepositoryPort;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Consulta e altera o aviso interno de estoque: quando ele sai e os minimos que separam os
 * niveis de cada tipo. Quem recebe nao se configura aqui: sao os administradores ativos.
 *
 * <p>Os minimos sao a regua unica do sistema: valem tambem para o painel e para a convocacao de
 * doadores. Por isso toda alteracao fica em auditoria com o nome de quem a fez.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigurarAvisoEstoqueUseCase {

    private static final int ULTIMA_HORA = 23;
    private static final int MINIMO_MAXIMO = 100_000;

    private final IAvisoEstoqueRepositoryPort avisoRepository;
    private final IEstoqueRepositoryPort estoqueRepository;
    private final AvisarEquipeDoEstoqueUseCase avisarEquipe;
    private final IAuditoriaPort auditoria;

    public ConfiguracaoAvisoEstoque consultar() {
        return avisoRepository.buscar();
    }

    /**
     * Grava a hora do relatorio diario. E o unico ajuste: o aviso em si nao se desliga.
     *
     * @param horaRelatorio hora cheia a partir da qual o relatorio sai, de 0 a 23
     * @param autor quem alterou
     * @return false quando nada mudou
     */
    public boolean salvar(final int horaRelatorio, final String autor) {
        if (horaRelatorio < 0 || horaRelatorio > ULTIMA_HORA) {
            throw new NegocioException(EstoqueErrorsMessage.AVISO_HORA_INVALIDA);
        }

        final ConfiguracaoAvisoEstoque vigente = avisoRepository.buscar();

        if (vigente.horaRelatorio() == horaRelatorio) {
            return false;
        }

        avisoRepository.salvar(horaRelatorio, autor, LocalDateTime.now(ConfiguracaoDisparoAutomatico.FUSO));
        auditoria.registrar("AVISO_ESTOQUE_ALTERADO", "relatorio diario a partir das %dh, antes %dh"
                .formatted(horaRelatorio, vigente.horaRelatorio()));
        return true;
    }

    /**
     * Grava os minimos de cada tipo. Nenhum e gravado se algum for invalido.
     *
     * @param minimos minimos informados na tela
     * @return quantos tipos tiveram os minimos alterados
     */
    public int salvarMinimos(final List<MinimosDoTipo> minimos) {
        minimos.forEach(this::validar);

        final List<MinimosDoTipo> alterados = new ArrayList<>();

        for (final MinimosDoTipo minimo : minimos) {
            if (estoqueRepository.atualizarMinimos(minimo.tipoSanguineo(), minimo.minimoCritico(), minimo.minimoBaixo())) {
                alterados.add(minimo);
            }
        }

        if (alterados.isEmpty()) {
            return 0;
        }

        auditoria.registrar("ESTOQUE_MINIMOS_ALTERADOS", alterados.stream()
                .map(minimo -> "%s: baixo < %d, critico < %d".formatted(minimo.tipoSanguineo().getSigla(),
                        minimo.minimoBaixo(), minimo.minimoCritico()))
                .reduce((primeiro, segundo) -> primeiro + "; " + segundo)
                .orElse(""));

        // Mudar a regua pode mudar o nivel sem nenhuma bolsa sair: a equipe e avisada do mesmo jeito.
        alterados.forEach(minimo -> {
            try {
                avisarEquipe.avaliarAlerta(minimo.tipoSanguineo());
            } catch (final RuntimeException excecao) {
                log.error("[m=salvarMinimos] Falha ao avaliar o alerta de {}", minimo.tipoSanguineo().getSigla(), excecao);
            }
        });

        return alterados.size();
    }

    private void validar(final MinimosDoTipo minimo) {
        if (minimo.minimoCritico() < 1 || minimo.minimoBaixo() <= minimo.minimoCritico()
                || minimo.minimoBaixo() > MINIMO_MAXIMO) {
            throw new NegocioException(EstoqueErrorsMessage.MINIMOS_INVALIDOS);
        }
    }
}
