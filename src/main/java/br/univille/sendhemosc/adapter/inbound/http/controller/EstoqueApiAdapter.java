package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.domain.dto.MovimentacaoEstoque;
import br.univille.sendhemosc.domain.dto.SituacaoEstoque;
import br.univille.sendhemosc.domain.port.outbound.IEstoqueRepositoryPort;
import br.univille.sendhemosc.usecase.estoque.ListarSituacaoEstoqueUseCase;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adapter de entrada HTTP para consulta do estoque de hemocomponentes. So leitura: a atualizacao
 * passa pela tela, que exige o token CSRF.
 */
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

    @GetMapping("/movimentacoes")
    public ResponseEntity<List<MovimentacaoEstoque>> movimentacoes(
            @RequestParam(defaultValue = "50") @Min(1) @Max(500) final int limite) {
        return ResponseEntity.ok(estoqueRepository.listarMovimentacoes(limite));
    }
}
