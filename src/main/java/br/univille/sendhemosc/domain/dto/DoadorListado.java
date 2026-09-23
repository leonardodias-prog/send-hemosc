package br.univille.sendhemosc.domain.dto;

import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import java.time.LocalDate;
import java.util.List;

/**
 * Linha da listagem de doadores, com a aptidao e os limites de contato ja avaliados.
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
 * @param convocacoesSemResposta convocacoes enviadas que ainda nao terminaram em doacao
 * @param ultimaConvocacao data da ultima convocacao enviada, nula para quem nunca recebeu
 * @param contato se ainda cabe convocar a pessoa agora
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
        List<String> motivosInaptidao,
        long convocacoesSemResposta,
        LocalDate ultimaConvocacao,
        LimiteDeContato contato) {

    /**
     * Indica se o doador pode ser convocado agora: apto, com consentimento dado e dentro dos
     * limites de contato.
     *
     * @return true quando a convocacao faz sentido
     */
    public boolean convocavel() {
        return apto && aceitaContato && contato.liberado();
    }
}
