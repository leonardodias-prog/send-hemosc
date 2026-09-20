package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.usecase.notificacao.DescadastrarDoadorUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Link de descadastro presente em todo e-mail enviado. Exigencia da LGPD: o titular precisa
 * conseguir revogar o consentimento sem depender de contato com a instituicao.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class DescadastroApiAdapter {

    private static final String PAGINA_CANCELADA =
            "<h1>Inscricao cancelada</h1><p>Voce nao recebera mais convocacoes do Send Hemosc.</p>";

    private static final String PAGINA_INVALIDA =
            "<h1>Nada a fazer</h1><p>Este link ja foi utilizado ou nao e valido.</p>";

    private final DescadastrarDoadorUseCase descadastrarDoador;

    @GetMapping(value = "/descadastro/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> descadastrar(@PathVariable final String token) {
        final boolean efetivado = descadastrarDoador.execute(token);
        final String corpo = efetivado ? PAGINA_CANCELADA : PAGINA_INVALIDA;

        return ResponseEntity.ok("<!DOCTYPE html><html lang=pt-BR><head><meta charset=UTF-8>"
                + "<title>Send Hemosc</title></head><body>" + corpo + "</body></html>");
    }
}
