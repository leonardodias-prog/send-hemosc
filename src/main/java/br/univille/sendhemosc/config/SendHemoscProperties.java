package br.univille.sendhemosc.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parametros de negocio do sistema, externalizados em application.yml.
 * Manter estas regras fora do codigo permite que a equipe de hemoterapia revise e ajuste
 * os valores sem alterar a aplicacao.
 *
 * @param aptidao regras de aptidao do doador
 * @param notificacao parametros de envio e reenvio de convocacoes
 */
@ConfigurationProperties(prefix = "sendhemosc")
public record SendHemoscProperties(Aptidao aptidao, Notificacao notificacao) {

    /**
     * Regras de aptidao para doacao.
     *
     * @param intervaloDiasMasculino intervalo minimo entre doacoes, em dias, para homens
     * @param intervaloDiasFeminino intervalo minimo entre doacoes, em dias, para mulheres
     * @param maxDoacoesAnoMasculino limite de doacoes em 12 meses para homens
     * @param maxDoacoesAnoFeminino limite de doacoes em 12 meses para mulheres
     * @param idadeMinima idade minima permitida
     * @param idadeMaxima idade maxima permitida
     * @param pesoMinimoKg peso minimo permitido, em quilos
     */
    public record Aptidao(
            int intervaloDiasMasculino,
            int intervaloDiasFeminino,
            int maxDoacoesAnoMasculino,
            int maxDoacoesAnoFeminino,
            int idadeMinima,
            int idadeMaxima,
            BigDecimal pesoMinimoKg) {
    }

    /**
     * Parametros de convocacao.
     *
     * @param intervaloReenvioDias dias minimos entre uma convocacao e a seguinte para a mesma pessoa
     * @param maxConvocacoesSemResposta convocacoes sem resposta a partir das quais a pessoa deixa de
     *                                  ser convocada
     * @param prazoTetoDias por quantos dias uma convocacao sem resposta conta para o teto; depois
     *                      disso deixa de pesar, e a pessoa volta a poder ser chamada
     * @param remetente endereco de origem dos e-mails
     * @param remetenteNome nome exibido como remetente
     * @param urlBase endereco publico da aplicacao, usado para montar o link de descadastro
     */
    public record Notificacao(int intervaloReenvioDias, int maxConvocacoesSemResposta, int prazoTetoDias,
                              String remetente, String remetenteNome, String urlBase) {

        private static final String CAMINHO_DO_LOGO = "/img/email/logo.png";

        /**
         * Endereco do logo que aparece no topo dos e-mails. Tem de ser absoluto e publico: a
         * mensagem e aberta fora do sistema, e o cliente de e-mail busca a imagem sem login.
         *
         * @return a URL completa da imagem
         */
        public String urlDoLogo() {
            return urlBase + CAMINHO_DO_LOGO;
        }
    }
}
