package br.univille.sendhemosc.usecase.usuario;

import br.univille.sendhemosc.config.SegurancaProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Limite de tentativas de entrada, contado por e-mail.
 *
 * <p>O BCrypt deixa cada tentativa lenta, mas nao impede quem testa senhas em sequencia. Depois
 * de algumas falhas seguidas o e-mail fica bloqueado por um tempo, com a senha certa ou nao. O
 * bloqueio vale para qualquer e-mail, com conta ou sem: se so conta existente bloqueasse, a
 * mensagem de bloqueio contaria a quem tenta quais e-mails tem cadastro.</p>
 *
 * <p>O contador vive em memoria e zera quando o servico reinicia. Para quem ataca, o reinicio
 * nao ajuda: ele so acontece depois de quinze minutos sem acesso nenhum.</p>
 */
@Component
public class LimiteDeTentativasDeLogin {

    /** Acima disto o mapa descarta o que ja nao conta, para nao crescer sem fim. */
    private static final int LIMPEZA_ACIMA_DE = 10_000;

    private final int maxFalhas;
    private final Duration bloqueio;
    private final Clock relogio;
    private final ConcurrentHashMap<String, Tentativas> porEmail = new ConcurrentHashMap<>();

    @Autowired
    public LimiteDeTentativasDeLogin(final SegurancaProperties seguranca) {
        this(seguranca, Clock.systemUTC());
    }

    LimiteDeTentativasDeLogin(final SegurancaProperties seguranca, final Clock relogio) {
        this.maxFalhas = seguranca.login().maxFalhas();
        this.bloqueio = Duration.ofMinutes(seguranca.login().bloqueioMinutos());
        this.relogio = relogio;
    }

    /**
     * Indica se o e-mail esta bloqueado agora.
     *
     * @param email e-mail digitado na entrada
     * @return true enquanto durar o bloqueio
     */
    public boolean bloqueado(final String email) {
        final Tentativas tentativas = porEmail.get(chave(email));

        return tentativas != null && tentativas.bloqueadaEm(relogio.instant());
    }

    /**
     * Conta uma falha de entrada.
     *
     * @param email e-mail digitado na entrada
     * @return true quando esta falha completou o limite e o e-mail acaba de ser bloqueado
     */
    public boolean registrarFalha(final String email) {
        final Instant agora = relogio.instant();
        final Tentativas depois = porEmail.compute(chave(email), (ignorado, antes) -> somar(antes, agora));

        limparSeNecessario(agora);

        return depois.falhas() == maxFalhas;
    }

    /**
     * Esquece as falhas do e-mail: entrada certa ou senha redefinida pelo link.
     *
     * @param email e-mail da conta
     */
    public void liberar(final String email) {
        porEmail.remove(chave(email));
    }

    public int getMaxFalhas() {
        return maxFalhas;
    }

    public long getBloqueioMinutos() {
        return bloqueio.toMinutes();
    }

    private Tentativas somar(final Tentativas antes, final Instant agora) {
        // A janela das falhas tem a duracao do bloqueio. Falha antiga deixa de somar, e bloqueio
        // vencido recomeca a contagem do zero.
        final boolean recomeca = antes == null
                || antes.ultimaFalhaEm().plus(bloqueio).isBefore(agora)
                || antes.bloqueioVencido(agora);
        final int falhas = recomeca ? 1 : antes.falhas() + 1;

        return new Tentativas(falhas, agora, falhas >= maxFalhas ? agora.plus(bloqueio) : null);
    }

    private void limparSeNecessario(final Instant agora) {
        if (porEmail.size() > LIMPEZA_ACIMA_DE) {
            porEmail.values().removeIf(tentativas -> !tentativas.bloqueadaEm(agora)
                    && tentativas.ultimaFalhaEm().plus(bloqueio).isBefore(agora));
        }
    }

    private static String chave(final String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Falhas acumuladas de um e-mail.
     *
     * @param falhas falhas seguidas dentro da janela
     * @param ultimaFalhaEm momento da falha mais recente
     * @param bloqueadoAte fim do bloqueio; nulo enquanto o limite nao foi atingido
     */
    private record Tentativas(int falhas, Instant ultimaFalhaEm, Instant bloqueadoAte) {

        boolean bloqueadaEm(final Instant agora) {
            return bloqueadoAte != null && bloqueadoAte.isAfter(agora);
        }

        boolean bloqueioVencido(final Instant agora) {
            return bloqueadoAte != null && !bloqueadoAte.isAfter(agora);
        }
    }
}
