package br.univille.sendhemosc.usecase.doador;

import br.univille.sendhemosc.domain.dto.NovoDoador;
import br.univille.sendhemosc.domain.exception.DoadorErrorsMessage;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Cadastra um doador. O e-mail e o identificador unico, entao a duplicidade e barrada aqui,
 * antes de chegar na restricao do banco, para que o usuario receba uma mensagem util.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CriarDoadorUseCase {

    private final IDoadorRepositoryPort doadorRepository;

    /**
     * Executa o cadastro.
     *
     * @param novoDoador dados informados
     * @return identificador do doador criado
     */
    public Long execute(final NovoDoador novoDoador) {
        final String email = novoDoador.email().trim().toLowerCase();

        if (doadorRepository.existeComEmail(email)) {
            throw new NegocioException(DoadorErrorsMessage.EMAIL_DUPLICADO);
        }

        final Long id = doadorRepository.salvar(new NovoDoador(
                novoDoador.nome().trim(),
                email,
                novoDoador.telefone(),
                novoDoador.tipoSanguineo(),
                novoDoador.sexo(),
                novoDoador.dataNascimento(),
                novoDoador.pesoKg(),
                novoDoador.aceitaContato()));

        log.info("[m=execute] Doador {} cadastrado, tipo {}, consentimento de contato={}",
                id, novoDoador.tipoSanguineo().getSigla(), novoDoador.aceitaContato());

        return id;
    }
}
