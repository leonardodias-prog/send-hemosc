package br.univille.sendhemosc.domain.dto;

import br.univille.sendhemosc.domain.enums.Sexo;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dados de cadastro de um doador.
 *
 * @param nome nome completo
 * @param email endereco de e-mail, usado como identificador unico
 * @param telefone telefone de contato, opcional
 * @param tipoSanguineo tipo sanguineo do doador
 * @param sexo sexo biologico, define intervalo e limite anual de doacoes
 * @param dataNascimento data de nascimento
 * @param pesoKg peso em quilos, opcional
 * @param aceitaContato consentimento explicito para receber convocacoes, exigido pela LGPD
 */
public record NovoDoador(
        @NotBlank(message = "{doador.nome.obrigatorio}")
        @Size(max = 150)
        String nome,

        @NotBlank(message = "{doador.email.obrigatorio}")
        @Email(message = "{doador.email.invalido}")
        @Size(max = 180)
        String email,

        @Size(max = 20)
        String telefone,

        @NotNull(message = "{doador.tipo-sanguineo.obrigatorio}")
        TipoSanguineo tipoSanguineo,

        @NotNull(message = "{doador.sexo.obrigatorio}")
        Sexo sexo,

        @NotNull(message = "{doador.data-nascimento.obrigatoria}")
        @Past(message = "{doador.data-nascimento.invalida}")
        LocalDate dataNascimento,

        BigDecimal pesoKg,

        boolean aceitaContato) {
}
