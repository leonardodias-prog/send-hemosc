package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.domain.dto.SituacaoEstoque;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.port.outbound.IEstoqueRepositoryPort;
import br.univille.sendhemosc.usecase.estoque.ListarSituacaoEstoqueUseCase;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adapter de entrada HTTP para consulta e atualizacao do estoque de hemocomponentes.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/estoque")
@RequiredArgsConstructor
public class EstoqueApiAdapter {

    private final ListarSituacaoEstoqueUseCase listarSituacaoEstoque;
    private final IEstoqueRepositoryPort estoqueRepository;

    @GetMapping
    public ResponseEntity<List<SituacaoEstoque>> listar() {
        return ResponseEntity.ok(listarSituacaoEstoque.execute());
    }

    @PutMapping("/{sigla}")
    public ResponseEntity<Void> atualizar(@PathVariable final String sigla,
                                          @RequestParam @Min(0) final int quantidadeBolsas) {
        estoqueRepository.atualizarQuantidade(TipoSanguineo.doSigla(sigla), quantidadeBolsas);
        return ResponseEntity.noContent().build();
    }
}
