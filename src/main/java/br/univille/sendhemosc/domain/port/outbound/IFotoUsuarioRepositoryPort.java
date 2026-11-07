package br.univille.sendhemosc.domain.port.outbound;

import br.univille.sendhemosc.domain.dto.FotoDoUsuario;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Porta de saida para as fotos de perfil. Cada conta tem no maximo uma.
 */
public interface IFotoUsuarioRepositoryPort {

    /**
     * Grava a foto da conta, substituindo a que existia.
     *
     * @param usuarioId conta dona da foto
     * @param tipoConteudo tipo de midia da imagem
     * @param conteudo bytes da imagem, ja tratados
     * @param quando momento da gravacao
     */
    void salvar(Long usuarioId, String tipoConteudo, byte[] conteudo, LocalDateTime quando);

    /**
     * Busca a foto com os bytes. Usado so ao entregar a imagem.
     *
     * @param usuarioId conta dona da foto
     * @return a foto, vazio quando a conta nao tem
     */
    Optional<FotoDoUsuario> buscar(Long usuarioId);

    /**
     * Informa se a conta tem foto e desde quando, sem ler os bytes.
     *
     * @param usuarioId conta dona da foto
     * @return o momento da gravacao, vazio quando a conta nao tem foto
     */
    Optional<LocalDateTime> momentoDaFoto(Long usuarioId);

    /**
     * Remove a foto da conta.
     *
     * @param usuarioId conta dona da foto
     * @return true se havia foto para remover
     */
    boolean remover(Long usuarioId);
}
