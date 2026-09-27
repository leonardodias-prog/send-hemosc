package br.univille.sendhemosc.usecase.notificacao;

import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Revoga o consentimento de contato do doador a partir do link presente em todo e-mail enviado.
 * Exigencia da LGPD e condicao para que a base de contatos possa ser usada.
 *
 * <p>Abrir o link so mostra a pagina; o cancelamento sai do botao dela. Filtros de e-mail que
 * abrem links sozinhos para inspecionar descadastrariam o doador sem ele saber.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DescadastrarDoadorUseCase {

    private final IDoadorRepositoryPort doadorRepository;

    /**
     * Consulta o link sem mudar nada: e o que decide o que a pagina mostra.
     *
     * @param token token recebido no link do e-mail
     * @return a situacao do contato do doador dono do link
     */
    public SituacaoDoLink consultar(final String token) {
        return doadorRepository.buscarPorToken(token)
                .map(doador -> doador.aceitaContato() ? SituacaoDoLink.RECEBE_CONVOCACOES : SituacaoDoLink.JA_CANCELADO)
                .orElse(SituacaoDoLink.INVALIDO);
    }

    /**
     * Processa o descadastro.
     *
     * @param token token recebido no link do e-mail
     * @return true se o descadastro foi efetivado
     */
    public boolean execute(final String token) {
        final boolean descadastrado = doadorRepository.descadastrarPorToken(token);
        log.info("[m=execute] Descadastro processado, efetivado={}", descadastrado);
        return descadastrado;
    }

    /**
     * O que o link de cancelamento encontra.
     */
    public enum SituacaoDoLink {

        /** O doador ainda autoriza contato: a pagina oferece o cancelamento. */
        RECEBE_CONVOCACOES,

        /** O contato ja foi cancelado antes: nada a fazer. */
        JA_CANCELADO,

        /** Nenhum doador tem este link, ou os dados ja foram excluidos. */
        INVALIDO
    }
}
