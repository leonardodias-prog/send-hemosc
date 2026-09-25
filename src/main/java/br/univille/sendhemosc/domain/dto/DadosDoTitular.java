package br.univille.sendhemosc.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Tudo o que o sistema guarda sobre um doador, no formato entregue a ele quando pede os proprios
 * dados (LGPD, art. 18). Fica de fora apenas o que e mecanismo interno, como o token do link de
 * descadastro.
 *
 * @param geradoEm quando a copia foi gerada
 * @param cadastro dados cadastrais
 * @param consentimentos historico de autorizacoes e revogacoes de contato
 * @param doacoes doacoes registradas
 * @param convocacoes convocacoes enviadas
 */
public record DadosDoTitular(
        LocalDateTime geradoEm,
        Cadastro cadastro,
        List<Consentimento> consentimentos,
        List<Doacao> doacoes,
        List<Convocacao> convocacoes) {

    /**
     * Dados cadastrais.
     *
     * @param nome nome completo
     * @param email endereco de e-mail
     * @param telefone telefone, opcional
     * @param tipoSanguineo sigla do tipo sanguineo
     * @param sexo sexo biologico
     * @param dataNascimento data de nascimento
     * @param pesoKg peso em quilos, opcional
     * @param aceitaContato se autoriza receber convocacoes hoje
     * @param ativo se o cadastro esta em uso
     * @param criadoEm quando foi cadastrado
     * @param atualizadoEm ultima alteracao
     */
    public record Cadastro(String nome, String email, String telefone, String tipoSanguineo, String sexo,
                           LocalDate dataNascimento, BigDecimal pesoKg, boolean aceitaContato, boolean ativo,
                           LocalDateTime criadoEm, LocalDateTime atualizadoEm) {

        /**
         * Converte o cadastro gravado para o formato exportado.
         *
         * @param doador cadastro gravado
         * @return dados cadastrais exportaveis
         */
        public static Cadastro de(final DoadorCadastrado doador) {
            return new Cadastro(doador.nome(), doador.email(), doador.telefone(), doador.tipoSanguineo().getSigla(),
                    doador.sexo().name(), doador.dataNascimento(), doador.pesoKg(), doador.aceitaContato(),
                    doador.ativo(), doador.criadoEm(), doador.atualizadoEm());
        }
    }

    /**
     * Uma manifestacao de consentimento.
     *
     * @param versaoTermo versao do termo vigente
     * @param aceito true quando autorizou, false quando revogou
     * @param origem de onde partiu
     * @param registradoEm quando
     */
    public record Consentimento(String versaoTermo, boolean aceito, String origem, LocalDateTime registradoEm) {
    }

    /**
     * Uma doacao registrada.
     *
     * @param dataDoacao data
     * @param localColeta local, opcional
     * @param observacao observacao, opcional
     */
    public record Doacao(LocalDate dataDoacao, String localColeta, String observacao) {
    }

    /**
     * Uma convocacao enviada.
     *
     * @param tipoSanguineoAlvo tipo que motivou a convocacao
     * @param nivelEstoque nivel do estoque no momento
     * @param status resultado do envio
     * @param enviadaEm quando foi enviada
     * @param compareceuEm data da doacao que a atendeu, se houve
     */
    public record Convocacao(String tipoSanguineoAlvo, String nivelEstoque, String status,
                             LocalDateTime enviadaEm, LocalDate compareceuEm) {
    }
}
