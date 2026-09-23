package br.univille.sendhemosc.usecase.doador;

import br.univille.sendhemosc.domain.dto.AptidaoDoador;
import br.univille.sendhemosc.domain.dto.CandidatoConvocacao;
import br.univille.sendhemosc.domain.dto.DoadorListado;
import br.univille.sendhemosc.domain.dto.FiltroDoador;
import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import br.univille.sendhemosc.usecase.notificacao.AvaliarLimiteDeContatoUseCase;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Lista doadores conforme os criterios da tela, avaliando a aptidao de cada um.
 *
 * <p>O filtro de aptidao nao vai para a consulta: a regra depende de sexo, idade, peso,
 * intervalo e limite anual, e existe em um unico lugar. Reproduzi-la em SQL criaria uma
 * segunda versao da mesma regra, que divergiria na primeira alteracao.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ListarDoadoresUseCase {

    private final IDoadorRepositoryPort doadorRepository;
    private final CalcularAptidaoUseCase calcularAptidao;
    private final AvaliarLimiteDeContatoUseCase avaliarLimiteDeContato;

    /**
     * Executa a listagem.
     *
     * @param filtro criterios informados na tela
     * @return doadores correspondentes, com a aptidao ja avaliada
     */
    public List<DoadorListado> execute(final FiltroDoador filtro) {
        final LocalDate hoje = LocalDate.now();

        final List<DoadorListado> listados = doadorRepository.buscarPorFiltro(filtro, hoje).stream()
                .map(candidato -> montar(candidato, hoje))
                .filter(doador -> !filtro.apenasAptos() || doador.apto())
                .toList();

        log.debug("[m=execute] Listagem devolveu {} doadores", listados.size());
        return listados;
    }

    private DoadorListado montar(final CandidatoConvocacao candidato, final LocalDate referencia) {
        final AptidaoDoador aptidao = calcularAptidao.execute(new CalcularAptidaoUseCase.DadosAptidao(
                candidato.sexo(),
                candidato.dataNascimento(),
                candidato.pesoKg(),
                candidato.ultimaDoacao(),
                candidato.doacoesUltimosDozeMeses(),
                referencia));

        return new DoadorListado(
                candidato.id(),
                candidato.nome(),
                candidato.email(),
                candidato.tipoSanguineo(),
                candidato.aceitaContato(),
                candidato.ultimaDoacao(),
                aptidao.apto(),
                aptidao.proximaDataApta(),
                aptidao.motivosInaptidao(),
                candidato.convocacoesSemResposta(),
                candidato.ultimaConvocacao() == null ? null : candidato.ultimaConvocacao().toLocalDate(),
                avaliarLimiteDeContato.execute(candidato.convocacoesSemResposta(),
                        candidato.ultimaConvocacao(), referencia));
    }
}
