package br.univille.sendhemosc.config;

import java.util.Arrays;

/**
 * Formas de entrega da notificacao, selecionadas por sendhemosc.email.modo.
 * Centralizar aqui evita que cada ponto do sistema compare textos soltos para descobrir
 * se o ambiente esta de fato enviando mensagens.
 */
public enum ModoEnvioEmail {

    /** Apenas registra a mensagem no log. Nada sai da aplicacao. */
    LOG(false),

    /** Entrega pelo servidor SMTP configurado. Bloqueado por varias hospedagens gratuitas. */
    SMTP(true),

    /** Entrega pela API HTTP do Resend, que nao depende de porta SMTP liberada. */
    RESEND(true);

    private final boolean envioReal;

    ModoEnvioEmail(final boolean envioReal) {
        this.envioReal = envioReal;
    }

    public boolean isEnvioReal() {
        return envioReal;
    }

    /**
     * Converte o valor da configuracao, tolerando caixa diferente e valor desconhecido.
     *
     * @param configurado valor de sendhemosc.email.modo
     * @return o modo correspondente, ou LOG quando nao reconhecido
     */
    public static ModoEnvioEmail de(final String configurado) {
        return Arrays.stream(values())
                .filter(modo -> modo.name().equalsIgnoreCase(configurado))
                .findFirst()
                .orElse(LOG);
    }

    /**
     * Indica se o valor configurado corresponde a um modo que entrega mensagens de verdade.
     *
     * @param configurado valor de sendhemosc.email.modo
     * @return true quando o ambiente envia de fato
     */
    public static boolean envioReal(final String configurado) {
        return de(configurado).isEnvioReal();
    }
}
