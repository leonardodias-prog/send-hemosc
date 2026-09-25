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
 * Exclui o doador e todo o historico dele: doacoes, convocacoes e consentimentos.
 *
 * <p>Atende dois pedidos diferentes. A equipe, para um cadastro que nao deveria existir. E o
 * proprio titular, que pela LGPD (art. 18, VI) pode pedir a eliminacao dos seus dados, o que nao
 * se confunde com parar de receber e-mails: descadastrar mantem o cadastro, excluir apaga.</p>
 *
 * <p>A auditoria registra que houve exclusao e de onde partiu, mas nao o nome nem o e-mail:
 * guardar quem pediu para ser apagado desfaria o proprio pedido.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcluirDoadorUseCase {

    private final IDoadorRepositoryPort doadorRepository;
    private final IAuditoriaPort auditoria;

    /**
     * Exclusao feita pela equipe.
     *
     * @param id doador
     */
    public void porId(final Long id) {
        excluir(doadorRepository.buscarPorId(id)
                .orElseThrow(() -> new NegocioException(DoadorErrorsMessage.NAO_ENCONTRADO)), "EQUIPE");
    }

    /**
     * Exclusao pedida pelo titular, pelo link recebido nos e-mails.
     *
     * @param token token do link
     */
    public void porToken(final String token) {
        excluir(doadorRepository.buscarPorToken(token)
                .orElseThrow(() -> new NegocioException(DoadorErrorsMessage.NAO_ENCONTRADO)), "TITULAR");
    }

    private void excluir(final DoadorCadastrado doador, final String origem) {
        doadorRepository.excluir(doador.id());
        auditoria.registrar("DOADOR_EXCLUIDO", "doador %d, a pedido de: %s".formatted(doador.id(), origem));

        log.info("[m=excluir] Doador {} excluido, origem {}", doador.id(), origem);
    }
}
