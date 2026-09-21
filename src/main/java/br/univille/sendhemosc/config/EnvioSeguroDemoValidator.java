package br.univille.sendhemosc.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

/**
 * Trava do ambiente de demonstracao publica.
 *
 * <p>O perfil demo roda com a massa ficticia carregada e fica acessivel a qualquer pessoa com o
 * link. Ligar o envio real nessas condicoes, sem restringir o destino, dispararia para toda a
 * base. Por isso, aqui o modo smtp so e aceito acompanhado de um destinatario unico: a
 * aplicacao recusa subir em qualquer outra combinacao, em vez de subir e enviar.</p>
 */
@Slf4j
@Profile("demo")
@Configuration
public class EnvioSeguroDemoValidator {

    private final String modo;
    private final String destinatarioTeste;

    public EnvioSeguroDemoValidator(@Value("${sendhemosc.email.modo:log}") final String modo,
                                    @Value("${sendhemosc.email.destinatario-teste:}") final String destinatarioTeste) {
        this.modo = modo;
        this.destinatarioTeste = destinatarioTeste;
    }

    /**
     * Verifica a combinacao de configuracao de envio antes de a aplicacao atender requisicoes.
     */
    @PostConstruct
    public void validar() {
        if (!ModoEnvioEmail.envioReal(modo)) {
            log.info("[m=validar] Demonstracao publica em modo log: nenhum e-mail sera enviado");
            return;
        }

        if (!StringUtils.hasText(destinatarioTeste)) {
            throw new IllegalStateException("""
                    Configuracao de envio recusada.

                    O perfil demo esta com EMAIL_MODO de envio real mas sem EMAIL_DESTINATARIO_TESTE.
                    Nessa combinacao as convocacoes iriam para o endereco de cada doador da massa
                    ficticia. Defina EMAIL_DESTINATARIO_TESTE com um unico endereco de teste, ou
                    volte EMAIL_MODO para log.""");
        }

        log.warn("[m=validar] Demonstracao publica com envio real: todas as mensagens irao "
                + "exclusivamente para {}", destinatarioTeste);
    }
}
