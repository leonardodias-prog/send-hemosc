package br.univille.sendhemosc.domain.dto;

import br.univille.sendhemosc.domain.enums.Sexo;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cadastro completo de um doador, como esta gravado. Usado na edicao e na exportacao dos dados
 * do titular; a listagem tem a sua propria linha, com a aptidao ja avaliada.
 *
 * @param id identificador
 * @param nome nome completo
 * @param email endereco de e-mail
 * @param telefone telefone, opcional
 * @param tipoSanguineo tipo sanguineo
 * @param sexo sexo biologico
 * @param dataNascimento data de nascimento
 * @param pesoKg peso em quilos, opcional
 * @param aceitaContato se autorizou receber convocacoes
 * @param ativo se o cadastro esta em uso; desativado nao aparece na busca nem e convocado
 * @param criadoEm quando foi cadastrado
 * @param atualizadoEm ultima alteracao
 */
public record DoadorCadastrado(
        Long id,
        String nome,
        String email,
        String telefone,
        TipoSanguineo tipoSanguineo,
        Sexo sexo,
        LocalDate dataNascimento,
        BigDecimal pesoKg,
        boolean aceitaContato,
        boolean ativo,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {

    /**
     * Os dados editaveis, no formato do formulario de cadastro.
     *
     * @return formulario preenchido com o cadastro atual
     */
    public NovoDoador comoFormulario() {
        return new NovoDoador(nome, email, telefone, tipoSanguineo, sexo, dataNascimento, pesoKg, aceitaContato);
    }
}
