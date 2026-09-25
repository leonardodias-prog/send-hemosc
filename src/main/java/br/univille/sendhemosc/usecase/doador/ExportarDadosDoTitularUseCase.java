package br.univille.sendhemosc.usecase.doador;

import br.univille.sendhemosc.domain.dto.DadosDoTitular;
import br.univille.sendhemosc.domain.dto.DoadorCadastrado;
import br.univille.sendhemosc.domain.exception.DoadorErrorsMessage;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import br.univille.sendhemosc.domain.port.outbound.IDadosDoTitularPort;
import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Reune tudo o que o sistema guarda sobre um doador, para entrega a ele.
 *
 * <p>A LGPD (art. 18, II e V) garante ao titular acesso aos dados e a portabilidade. A copia
 * pode ser pedida pelo proprio doador, pelo link dos e-mails, ou gerada pela equipe quando o
 * pedido chega por outro canal. Nos dois casos fica em auditoria.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportarDadosDoTitularUseCase {

    private final IDoadorRepositoryPort doadorRepository;
    private final IDadosDoTitularPort dadosDoTitular;
    private final IAuditoriaPort auditoria;

    /**
     * Exportacao gerada pela equipe.
     *
     * @param id doador
     * @return dados do titular
     */
    public DadosDoTitular porId(final Long id) {
        return exportar(doadorRepository.buscarPorId(id)
                .orElseThrow(() -> new NegocioException(DoadorErrorsMessage.NAO_ENCONTRADO)), "EQUIPE");
    }

    /**
     * Exportacao pedida pelo titular, pelo link recebido nos e-mails.
     *
     * @param token token do link
     * @return dados do titular
     */
    public DadosDoTitular porToken(final String token) {
        return exportar(doadorRepository.buscarPorToken(token)
                .orElseThrow(() -> new NegocioException(DoadorErrorsMessage.NAO_ENCONTRADO)), "TITULAR");
    }

    private DadosDoTitular exportar(final DoadorCadastrado doador, final String origem) {
        final DadosDoTitular dados = new DadosDoTitular(
                LocalDateTime.now(),
                DadosDoTitular.Cadastro.de(doador),
                dadosDoTitular.consentimentos(doador.id()),
                dadosDoTitular.doacoes(doador.id()),
                dadosDoTitular.convocacoes(doador.id()));

        auditoria.registrar("DADOS_DO_TITULAR_EXPORTADOS",
                "doador %d, a pedido de: %s".formatted(doador.id(), origem));

        log.info("[m=exportar] Dados do doador {} exportados, origem {}", doador.id(), origem);
        return dados;
    }
}
