package br.univille.sendhemosc.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Publica em /actuator/info como o envio de e-mail esta configurado.
 *
 * <p>Sem isso, descobrir se um ambiente esta apenas registrando as mensagens ou enviando de
 * verdade exige ler log de inicializacao ou inspecionar o HTML do painel, o que ja levou a
 * diagnostico errado. O endereco de redirecionamento aparece mascarado: basta conferir que
 * existe e qual o dominio, sem expor a caixa de ninguem.</p>
 */
@Component
public class InfoEnvioContributor implements InfoContributor {

    private final String modo;
    private final String destinatarioTeste;

    public InfoEnvioContributor(@Value("${sendhemosc.email.modo:log}") final String modo,
                                @Value("${sendhemosc.email.destinatario-teste:}") final String destinatarioTeste) {
        this.modo = modo;
        this.destinatarioTeste = destinatarioTeste;
    }

    @Override
    public void contribute(final Info.Builder builder) {
        final Map<String, Object> envio = new LinkedHashMap<>();
        final boolean envioReal = "smtp".equalsIgnoreCase(modo);

        envio.put("modo", modo);
        envio.put("envioReal", envioReal);
        envio.put("descricao", envioReal
                ? "As mensagens sao entregues pelo servidor SMTP configurado"
                : "As mensagens sao apenas registradas em log e nao saem da aplicacao");

        final String[] destinos = StringUtils.hasText(destinatarioTeste)
                ? destinatarioTeste.split(",")
                : new String[0];

        envio.put("redirecionamentoAtivo", destinos.length > 0);
        envio.put("totalDestinatariosDeTeste", destinos.length);
        if (destinos.length > 0) {
            envio.put("destinatariosDeTeste", java.util.Arrays.stream(destinos)
                    .map(String::trim)
                    .map(InfoEnvioContributor::mascarar)
                    .toList());
        }

        builder.withDetail("email", envio);
    }

    /**
     * Reduz o endereco ao suficiente para conferencia, sem expor a caixa por inteiro.
     *
     * @param endereco endereco configurado
     * @return endereco mascarado, por exemplo le***@gmail.com
     */
    private static String mascarar(final String endereco) {
        final int arroba = endereco.indexOf('@');
        if (arroba <= 0) {
            return "***";
        }

        final String usuario = endereco.substring(0, arroba);
        final String visivel = usuario.length() <= 2 ? usuario.substring(0, 1) : usuario.substring(0, 2);

        return visivel + "***" + endereco.substring(arroba);
    }
}
