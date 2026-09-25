package br.univille.sendhemosc.usecase.doador;

import br.univille.sendhemosc.domain.dto.DoadorCadastrado;
import br.univille.sendhemosc.domain.dto.NovoDoador;
import br.univille.sendhemosc.domain.exception.DoadorErrorsMessage;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IConsentimentoPort;
import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Corrige o cadastro de um doador. Aplica as mesmas normalizacoes do cadastro e a mesma regra
 * de e-mail unico, agora ignorando o proprio doador.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EditarDoadorUseCase {

    private final IDoadorRepositoryPort doadorRepository;
    private final IConsentimentoPort consentimento;

    /**
     * Executa a edicao.
     *
     * @param id doador a editar
     * @param dados novos dados cadastrais
     */
    public void execute(final Long id, final NovoDoador dados) {
        final DoadorCadastrado atual = doadorRepository.buscarPorId(id)
                .orElseThrow(() -> new NegocioException(DoadorErrorsMessage.NAO_ENCONTRADO));
        final String email = dados.email().trim().toLowerCase();

        if (doadorRepository.existeComEmailEmOutro(email, id)) {
            throw new NegocioException(DoadorErrorsMessage.EMAIL_DUPLICADO);
        }

        doadorRepository.atualizar(id, new NovoDoador(
                dados.nome().trim(),
                email,
                dados.telefone(),
                dados.tipoSanguineo(),
                dados.sexo(),
                dados.dataNascimento(),
                dados.pesoKg(),
                dados.aceitaContato()));

        // Mudar a autorizacao de contato pela edicao tambem precisa ficar provado, como no
        // cadastro e no link de descadastro. Mudar outro campo nao e manifestacao de consentimento.
        if (atual.aceitaContato() != dados.aceitaContato()) {
            consentimento.registrar(id, dados.aceitaContato(), "EDICAO");
        }

        log.info("[m=execute] Doador {} editado", id);
    }
}
