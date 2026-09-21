package br.univille.sendhemosc.adapter.outbound.email;

import br.univille.sendhemosc.domain.dto.MensagemEmail;
import java.util.Arrays;
import java.util.List;
import org.springframework.util.StringUtils;

/**
 * Trava de seguranca comum aos adapters que entregam mensagens de verdade.
 *
 * <p>Com um ou mais enderecos configurados, toda mensagem passa a ir exclusivamente para eles,
 * seja qual for o doador. Existe porque o sistema roda com massa ficticia carregada: sem
 * restringir o destino, um unico clique dispararia para a base inteira.</p>
 *
 * <p>Fica em uma classe propria para que a regra exista em um lugar so. Duplicada entre
 * adapters, bastaria um deles esquecer de aplica-la para a protecao deixar de valer.</p>
 */
public final class RedirecionamentoDeTeste {

    private final List<String> destinatarios;

    private RedirecionamentoDeTeste(final List<String> destinatarios) {
        this.destinatarios = destinatarios;
    }

    /**
     * Monta a trava a partir do valor bruto da configuracao.
     *
     * @param configurado um endereco, varios separados por virgula, ou vazio
     * @return a trava correspondente, inativa quando nada foi configurado
     */
    public static RedirecionamentoDeTeste de(final String configurado) {
        if (!StringUtils.hasText(configurado)) {
            return new RedirecionamentoDeTeste(List.of());
        }

        return new RedirecionamentoDeTeste(Arrays.stream(configurado.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList());
    }

    public boolean isAtivo() {
        return !destinatarios.isEmpty();
    }

    public List<String> getDestinatarios() {
        return destinatarios;
    }

    /**
     * Para onde a mensagem deve ir de fato.
     *
     * @param mensagem mensagem originalmente endereçada ao doador
     * @return os enderecos de teste quando a trava esta ativa, senao o do proprio doador
     */
    public List<String> destinosPara(final MensagemEmail mensagem) {
        return isAtivo() ? destinatarios : List.of(mensagem.destinatario());
    }

    /**
     * Assunto a ser usado. Com a trava ativa, preserva o destinatario original para conferencia.
     *
     * @param mensagem mensagem a enviar
     * @return assunto final
     */
    public String assuntoPara(final MensagemEmail mensagem) {
        return isAtivo()
                ? "[TESTE -> %s] %s".formatted(mensagem.destinatario(), mensagem.assunto())
                : mensagem.assunto();
    }

    /**
     * Descricao dos destinos, para registro em log.
     *
     * @return enderecos separados por virgula
     */
    public String descricao() {
        return String.join(", ", destinatarios);
    }
}
