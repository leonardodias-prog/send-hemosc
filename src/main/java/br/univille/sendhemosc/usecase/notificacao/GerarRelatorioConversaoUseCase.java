package br.univille.sendhemosc.usecase.notificacao;

import br.univille.sendhemosc.domain.dto.RelatorioConversao;
import br.univille.sendhemosc.domain.port.outbound.INotificacaoRepositoryPort;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Monta o relatorio de quantos convocados viraram doacao, por tipo sanguineo, em um periodo.
 */
@Service
@RequiredArgsConstructor
public class GerarRelatorioConversaoUseCase {

    private final INotificacaoRepositoryPort notificacaoRepository;

    /**
     * Gera o relatorio.
     *
     * @param inicio data inicial do periodo, inclusive
     * @param fim data final do periodo, inclusive
     * @return relatorio com uma linha por tipo sanguineo
     */
    public RelatorioConversao execute(final LocalDate inicio, final LocalDate fim) {
        return new RelatorioConversao(inicio, fim, notificacaoRepository.contarConversaoPorTipo(inicio, fim));
    }
}
