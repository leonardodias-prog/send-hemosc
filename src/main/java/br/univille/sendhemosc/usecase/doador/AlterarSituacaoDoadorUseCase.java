package br.univille.sendhemosc.usecase.doador;

import br.univille.sendhemosc.domain.dto.DoadorCadastrado;
import br.univille.sendhemosc.domain.exception.DoadorErrorsMessage;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Desativa ou reativa um doador.
 *
 * <p>Desativar e o caminho para quem deixou de doar, mudou de cidade ou teve o cadastro feito
 * por engano, sem apagar o historico: o doador some da busca e das convocacoes, e pode voltar.
 * Apagar de vez e outra acao, a exclusao.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlterarSituacaoDoadorUseCase {

    private final IDoadorRepositoryPort doadorRepository;
    private final IAuditoriaPort auditoria;

    /**
     * Executa a mudanca de situacao.
     *
     * @param id doador
     * @param ativo true para reativar, false para desativar
     * @return nome do doador, para a confirmacao na tela
     */
    public String execute(final Long id, final boolean ativo) {
        final DoadorCadastrado doador = doadorRepository.buscarPorId(id)
                .orElseThrow(() -> new NegocioException(DoadorErrorsMessage.NAO_ENCONTRADO));

        doadorRepository.definirAtivo(id, ativo);
        auditoria.registrar(ativo ? "DOADOR_REATIVADO" : "DOADOR_DESATIVADO",
                "%s (doador %d)".formatted(doador.nome(), id));

        log.info("[m=execute] Doador {} {}", id, ativo ? "reativado" : "desativado");
        return doador.nome();
    }
}
