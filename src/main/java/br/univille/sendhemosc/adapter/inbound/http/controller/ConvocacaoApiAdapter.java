package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.domain.dto.ResultadoConvocacao;
import br.univille.sendhemosc.domain.enums.OrigemNotificacao;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.usecase.notificacao.ConvocarDoadoresUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adapter de entrada HTTP para o disparo manual de convocacoes pela equipe de captacao.
 */
@Slf4j
@RestController
@RequestMapping("/api/convocacoes")
@RequiredArgsConstructor
public class ConvocacaoApiAdapter {

    private final ConvocarDoadoresUseCase convocarDoadores;

    @PostMapping("/{sigla}")
    public ResponseEntity<ResultadoConvocacao> convocar(
            @PathVariable final String sigla,
            @RequestParam(defaultValue = "false") final boolean ignorarNivel) {

        log.info("[m=convocar] Convocacao manual solicitada para o tipo {}", sigla);
        final ResultadoConvocacao resultado = convocarDoadores.execute(
                TipoSanguineo.doSigla(sigla), OrigemNotificacao.MANUAL, ignorarNivel);

        return ResponseEntity.ok(resultado);
    }
}
