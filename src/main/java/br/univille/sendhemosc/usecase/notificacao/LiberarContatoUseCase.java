package br.univille.sendhemosc.usecase.notificacao;

import br.univille.sendhemosc.domain.dto.CandidatoConvocacao;
import br.univille.sendhemosc.domain.exception.DoadorErrorsMessage;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Libera o limite de contato de um doador, a pedido do administrador.
 *
 * <p>O teto de convocacoes sem resposta se desfaz sozinho com o prazo, ou com uma doacao. Nem
 * sempre da para esperar: a pessoa pode ter avisado que quer voltar a ser chamada, ou a equipe
 * falou com ela por outro canal. Liberar faz as convocacoes enviadas ate aqui deixarem de contar,
 * para o teto e para o intervalo, e fica em auditoria com o nome de quem liberou.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiberarContatoUseCase {

    private final IDoadorRepositoryPort doadorRepository;
    private final IAuditoriaPort auditoria;

    /**
     * Executa a liberacao.
     *
     * @param doadorId doador a liberar
     * @return nome do doador, para a confirmacao na tela
     */
    public String execute(final Long doadorId) {
        final CandidatoConvocacao doador = doadorRepository
                .buscarPorIdentificadores(Set.of(doadorId), LocalDate.now()).stream()
                .findFirst()
                .orElseThrow(() -> new NegocioException(DoadorErrorsMessage.NAO_ENCONTRADO));

        // Mesmo relogio de quando cada convocacao e registrada, para a comparacao valer.
        doadorRepository.liberarContato(doadorId, LocalDateTime.now());

        auditoria.registrar("LIMITE_DE_CONTATO_LIBERADO",
                "%s (doador %d): convocacoes anteriores deixam de contar para o teto e o intervalo"
                        .formatted(doador.nome(), doadorId));

        return doador.nome();
    }
}
