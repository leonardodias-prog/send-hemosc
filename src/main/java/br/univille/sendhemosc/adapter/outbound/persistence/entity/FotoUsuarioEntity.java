package br.univille.sendhemosc.adapter.outbound.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Foto de perfil de uma conta, no maximo uma por conta. Separada da conta de proposito: os bytes
 * da imagem so sao lidos quando a foto e exibida, e nao a cada consulta de usuario.
 */
@Entity
@Table(name = "usuario_foto")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FotoUsuarioEntity {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "tipo_conteudo", nullable = false, length = 30)
    private String tipoConteudo;

    @Column(nullable = false)
    private byte[] conteudo;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}
