package br.univille.sendhemosc.usecase.notificacao;

import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Revoga o consentimento de contato do doador a partir do link presente em todo e-mail enviado.
 * Exigencia da LGPD e condicao para que a base de contatos possa ser usada.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DescadastrarDoadorUseCase {

    private final IDoadorRepositoryPort doadorRepository;

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
}
