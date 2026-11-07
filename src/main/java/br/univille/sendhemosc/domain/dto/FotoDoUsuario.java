package br.univille.sendhemosc.domain.dto;

import java.time.LocalDateTime;

/**
 * Foto de perfil como esta guardada: ja reduzida e recodificada, pronta para ser entregue.
 *
 * @param tipoConteudo tipo de midia da imagem, sempre o que o sistema gravou
 * @param conteudo bytes da imagem
 * @param atualizadoEm quando a foto foi gravada
 */
public record FotoDoUsuario(String tipoConteudo, byte[] conteudo, LocalDateTime atualizadoEm) {
}
