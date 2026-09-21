package br.univille.sendhemosc.domain.dto;

import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import java.time.LocalDate;
import java.util.List;

/**
 * Linha da listagem de doadores, com a aptidao ja avaliada.
 *
 * @param id identificador
 * @param nome nome completo
 * @param email endereco de e-mail
 * @param tipoSanguineo tipo sanguineo
 * @param aceitaContato se autorizou receber convocacoes
 * @param ultimaDoacao data da ultima doacao, nula para quem nunca doou
 * @param apto se pode doar hoje
 * @param proximaDataApta data em que volta a poder doar
 * @param motivosInaptidao chaves dos motivos, vazia quando apto
 */
public record DoadorListado(
        Long id,
        String nome,
        String email,
        TipoSanguineo tipoSanguineo,
        boolean aceitaContato,
        LocalDate ultimaDoacao,
        boolean apto,
        LocalDate proximaDataApta,
        List<String> motivosInaptidao) {

    /**
     * Indica se o doador pode ser convocado agora: apto e com consentimento dado.
     *
     * @return true quando a convocacao faz sentido
     */
    public boolean convocavel() {
        return apto && aceitaContato;
    }
}
