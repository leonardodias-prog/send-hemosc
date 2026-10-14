package br.univille.sendhemosc.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.AuditoriaEntity;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.AuditoriaJpaRepository;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.DoadorJpaRepository;
import br.univille.sendhemosc.domain.dto.NovoDoador;
import br.univille.sendhemosc.domain.enums.Sexo;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import br.univille.sendhemosc.usecase.doador.ExcluirDoadorUseCase;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * A exclusao a pedido do titular nao pode deixar o nome dele na auditoria, inclusive nos registros
 * gravados antes de a auditoria passar a guardar so o identificador.
 */
@SpringBootTest
@Transactional
@DisplayName("Exclusao do doador retira o nome dele da auditoria")
class AnonimizacaoDoDoadorTest {

    @Autowired
    private IDoadorRepositoryPort doadorRepository;

    @Autowired
    private IAuditoriaPort auditoria;

    @Autowired
    private AuditoriaJpaRepository auditoriaRepository;

    @Autowired
    private ExcluirDoadorUseCase excluirDoador;

    @Autowired
    private DoadorJpaRepository doadorJpa;

    private List<String> detalhesDe(final String acao) {
        return auditoriaRepository.findAll().stream()
                .filter(registro -> acao.equals(registro.getAcao()))
                .map(AuditoriaEntity::getDetalhe)
                .toList();
    }

    @Test
    @DisplayName("registros antigos com o nome passam a citar so o identificador")
    void registrosAntigosPerdemONome() {
        final String nome = "Titular " + UUID.randomUUID();
        final Long id = doadorRepository.salvar(new NovoDoador(nome, UUID.randomUUID() + "@example.org", null,
                TipoSanguineo.O_NEGATIVO, Sexo.FEMININO, LocalDate.of(1990, 1, 1), null, true));
        // Formato gravado antes da correcao, com o nome por extenso.
        auditoria.registrar("DOACAO_REGISTRADA", nome + " doou em 2026-09-01; 1 convocacao(oes) fechada(s)");
        auditoria.registrar("DOADOR_DESATIVADO", nome + " (doador " + id + ")");

        excluirDoador.porToken(doadorJpa.findById(id).orElseThrow().getTokenDescadastro());

        assertThat(detalhesDe("DOACAO_REGISTRADA"))
                .noneMatch(detalhe -> detalhe.contains(nome))
                .contains("doador " + id + " doou em 2026-09-01; 1 convocacao(oes) fechada(s)");
        assertThat(detalhesDe("DOADOR_DESATIVADO")).noneMatch(detalhe -> detalhe.contains(nome));
    }

    @Test
    @DisplayName("registros de contas da equipe com o mesmo nome ficam como estao")
    void registrosDaEquipeNaoMudam() {
        final String nome = "Homonimo " + UUID.randomUUID();
        final Long id = doadorRepository.salvar(new NovoDoador(nome, UUID.randomUUID() + "@example.org", null,
                TipoSanguineo.A_POSITIVO, Sexo.MASCULINO, LocalDate.of(1985, 5, 5), null, true));
        auditoria.registrar("CONTA_CRIADA", nome + " (equipe@example.org) como OPERADOR");

        excluirDoador.porId(id);

        assertThat(detalhesDe("CONTA_CRIADA")).contains(nome + " (equipe@example.org) como OPERADOR");
    }
}
