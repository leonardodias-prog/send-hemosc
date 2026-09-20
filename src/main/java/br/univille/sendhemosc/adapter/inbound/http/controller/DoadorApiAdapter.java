package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.domain.dto.NovoDoador;
import br.univille.sendhemosc.usecase.doador.CriarDoadorUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adapter de entrada HTTP para o cadastro de doadores.
 */
@Slf4j
@RestController
@RequestMapping("/api/doadores")
@RequiredArgsConstructor
public class DoadorApiAdapter {

    private final CriarDoadorUseCase criarDoador;

    @PostMapping
    public ResponseEntity<Map<String, Long>> cadastrar(@Valid @RequestBody final NovoDoador novoDoador) {
        final Long id = criarDoador.execute(novoDoador);

        return ResponseEntity.created(URI.create("/api/doadores/" + id)).body(Map.of("id", id));
    }
}
