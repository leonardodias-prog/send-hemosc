package br.univille.sendhemosc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Parametros de protecao das contas, externalizados em application.yml.
 *
 * <p>Os valores padrao valem quando a chave nao existe no arquivo. E o caso dos testes: o
 * application.yml de teste substitui o principal por inteiro, e nao precisa repetir o que ja
 * tem valor razoavel.</p>
 *
 * @param login limite de tentativas de entrada
 * @param recuperacaoSenha validade e limites de envio do link de recuperacao de senha
 * @param aprovacao validade do link de aprovacao e ritmo do aviso aos administradores
 */
@ConfigurationProperties(prefix = "sendhemosc.seguranca")
public record SegurancaProperties(
        @DefaultValue Login login,
        @DefaultValue RecuperacaoSenha recuperacaoSenha,
        @DefaultValue Aprovacao aprovacao) {

    /**
     * Limite de tentativas de entrada, contado por e-mail.
     *
     * @param maxFalhas falhas seguidas que bloqueiam o e-mail
     * @param bloqueioMinutos quanto dura o bloqueio; e tambem a janela em que as falhas se somam
     */
    public record Login(@DefaultValue("5") int maxFalhas, @DefaultValue("15") int bloqueioMinutos) {
    }

    /**
     * Link de recuperacao de senha. Todo pedido atendido vira um e-mail, entao os limites
     * protegem tanto a caixa de quem e alvo de pedidos repetidos quanto a cota diaria do provedor.
     *
     * @param validadeMinutos por quanto tempo o link vale
     * @param intervaloMinutos tempo minimo entre dois envios para a mesma conta
     * @param maxPorContaDia envios para a mesma conta em 24 horas
     * @param maxNoSistemaDia envios somando todas as contas em 24 horas
     */
    public record RecuperacaoSenha(@DefaultValue("60") int validadeMinutos,
                                   @DefaultValue("15") int intervaloMinutos,
                                   @DefaultValue("3") int maxPorContaDia,
                                   @DefaultValue("30") int maxNoSistemaDia) {
    }

    /**
     * Aprovacao de cadastro de responsavel.
     *
     * @param validadeDias por quantos dias o link de aprovacao vale, contados do cadastro
     * @param avisoIntervaloMinutos tempo minimo entre dois avisos de cadastros pendentes
     * @param avisoMaxListados quantos cadastros um aviso lista; os demais entram so na contagem
     */
    public record Aprovacao(@DefaultValue("7") int validadeDias,
                            @DefaultValue("60") int avisoIntervaloMinutos,
                            @DefaultValue("10") int avisoMaxListados) {
    }
}
