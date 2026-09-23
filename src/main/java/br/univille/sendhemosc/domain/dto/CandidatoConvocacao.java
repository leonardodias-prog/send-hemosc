package br.univille.sendhemosc.domain.dto;

import br.univille.sendhemosc.domain.enums.Sexo;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Doador candidato a receber uma convocacao, com os dados necessarios para avaliar sua aptidao e
 * os limites de contato. A avaliacao em si nao acontece aqui: aptidao e com CalcularAptidaoUseCase,
 * limite de contato com AvaliarLimiteDeContatoUseCase, cada regra em um lugar so.
 *
 * @param id identificador do doador
 * @param nome nome do doador
 * @param email endereco de e-mail
 * @param tipoSanguineo tipo sanguineo do doador
 * @param tokenDescadastro token usado no link de descadastro
 * @param aceitaContato se o doador autorizou receber convocacoes
 * @param sexo sexo biologico, define intervalo e limite anual
 * @param dataNascimento data de nascimento
 * @param pesoKg peso em quilos, pode ser nulo
 * @param ultimaDoacao data da ultima doacao, nula para quem nunca doou
 * @param doacoesUltimosDozeMeses total de doacoes nos ultimos doze meses
 * @param convocacoesSemResposta convocacoes sem resposta dentro do prazo do teto, depois da ultima
 *                               liberacao manual
 * @param ultimaConvocacao quando a ultima convocacao foi enviada depois da ultima liberacao manual,
 *                         nula para quem nunca recebeu
 */
public record CandidatoConvocacao(
        Long id,
        String nome,
        String email,
        TipoSanguineo tipoSanguineo,
        String tokenDescadastro,
        boolean aceitaContato,
        Sexo sexo,
        LocalDate dataNascimento,
        BigDecimal pesoKg,
        LocalDate ultimaDoacao,
        long doacoesUltimosDozeMeses,
        long convocacoesSemResposta,
        LocalDateTime ultimaConvocacao) {
}
