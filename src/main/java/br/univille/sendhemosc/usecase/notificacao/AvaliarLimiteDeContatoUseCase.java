package br.univille.sendhemosc.usecase.notificacao;

import br.univille.sendhemosc.config.SendHemoscProperties;
import br.univille.sendhemosc.domain.dto.LimiteDeContato;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Decide se ainda cabe convocar um doador, pelo historico de contato com ele.
 *
 * <p>Aptidao responde se a pessoa pode doar; esta regra responde se ainda cabe insistir. Sao
 * perguntas diferentes: quem recebeu tres convites e nao apareceu nao deve receber o quarto, e
 * quem foi convidado ontem nao deve ser convidado de novo hoje, mesmo com o estoque ainda baixo.
 * Sem esta regra, uma rodada diaria mandaria e-mail para as mesmas pessoas todos os dias.</p>
 *
 * <p>Vale para toda convocacao, automatica ou manual. Do lado de quem recebe o e-mail e o mesmo,
 * seja qual for o botao que o disparou.</p>
 *
 * <p>O teto nao e permanente. Desfaz-se de tres formas: uma doacao registrada fecha as pendentes;
 * cada convocacao sem resposta deixa de contar depois do prazo configurado; e o administrador pode
 * liberar a pessoa na hora. As duas ultimas ja chegam aplicadas na contagem que a consulta traz.</p>
 *
 * <p>Como a regra de aptidao, e pura e sem acesso a banco: os dois limites ficam legiveis aqui,
 * e os valores, em application.yml.</p>
 */
@Service
@RequiredArgsConstructor
public class AvaliarLimiteDeContatoUseCase {

    private final SendHemoscProperties properties;

    /**
     * Avalia os limites de contato.
     *
     * @param convocacoesSemResposta convocacoes sem resposta dentro do prazo, depois da ultima liberacao
     * @param ultimaConvocacao quando a ultima convocacao foi enviada, nulo se nunca foi
     * @param referencia data em que a convocacao aconteceria
     * @return se a pessoa pode ser convocada e, quando nao pode, por qual dos limites
     */
    public LimiteDeContato execute(final long convocacoesSemResposta, final LocalDateTime ultimaConvocacao,
                                   final LocalDate referencia) {
        final SendHemoscProperties.Notificacao regras = properties.notificacao();

        // O teto vem antes: quem ja ignorou convites demais nao volta a ser chamado so porque o
        // intervalo passou. Ele se desfaz com doacao, com o prazo ou com a liberacao manual.
        if (convocacoesSemResposta >= regras.maxConvocacoesSemResposta()) {
            return LimiteDeContato.noTeto();
        }

        if (ultimaConvocacao != null) {
            final LocalDate liberadoEm = ultimaConvocacao.toLocalDate().plusDays(regras.intervaloReenvioDias());

            if (liberadoEm.isAfter(referencia)) {
                return LimiteDeContato.emIntervalo(liberadoEm);
            }
        }

        return LimiteDeContato.livre();
    }
}
