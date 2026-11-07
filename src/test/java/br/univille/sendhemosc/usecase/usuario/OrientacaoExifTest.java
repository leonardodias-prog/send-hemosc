package br.univille.sendhemosc.usecase.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import br.univille.sendhemosc.FotosDeTeste;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.function.BiFunction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Orientacao gravada pela camera")
class OrientacaoExifTest {

    @Test
    @DisplayName("le os oito valores, nas duas ordens de byte que o formato permite")
    void lePeloMenosOsOitoValoresNosDoisSentidos() {
        for (int valor = 1; valor <= 8; valor++) {
            assertThat(OrientacaoExif.ler(FotosDeTeste.jpegComOrientacao(40, 20, valor, true)))
                    .as("pequeno primeiro, valor %d", valor).isEqualTo(valor);
            assertThat(OrientacaoExif.ler(FotosDeTeste.jpegComOrientacao(40, 20, valor, false)))
                    .as("grande primeiro, valor %d", valor).isEqualTo(valor);
        }
    }

    @Test
    @DisplayName("foto sem a anotacao, ou com valor sem sentido, fica como esta")
    void semAnotacaoOuForaDaFaixa() {
        assertThat(OrientacaoExif.ler(FotosDeTeste.jpeg(40, 20))).isEqualTo(1);
        assertThat(OrientacaoExif.ler(FotosDeTeste.jpegComOrientacao(40, 20, 0, true))).isEqualTo(1);
        assertThat(OrientacaoExif.ler(FotosDeTeste.jpegComOrientacao(40, 20, 9, true))).isEqualTo(1);
        assertThat(OrientacaoExif.ler(FotosDeTeste.png(40, 20))).isEqualTo(1);
        assertThat(OrientacaoExif.ler(new byte[0])).isEqualTo(1);
    }

    @Test
    @DisplayName("arquivo cortado em qualquer ponto nunca quebra a leitura")
    void arquivoCortadoNaoQuebra() {
        final byte[] inteiro = FotosDeTeste.jpegComOrientacao(40, 20, 6, true);

        for (int tamanho = 0; tamanho <= 80; tamanho++) {
            final int lido = OrientacaoExif.ler(Arrays.copyOf(inteiro, tamanho));

            assertThat(lido).as("cortado em %d bytes", tamanho).isBetween(1, 8);
        }
    }

    @Test
    @DisplayName("bytes aleatorios depois do marcador de inicio nao quebram a leitura")
    void lixoNaoQuebra() {
        final byte[] lixo = new byte[200];
        new java.util.Random(7).nextBytes(lixo);
        lixo[0] = (byte) 0xFF;
        lixo[1] = (byte) 0xD8;

        assertThat(OrientacaoExif.ler(lixo)).isBetween(1, 8);
    }

    @Test
    @DisplayName("cada orientacao leva cada pixel ao lugar que o padrao EXIF define")
    void cadaOrientacaoMoveOsPixelsCertos() {
        final int largura = 3;
        final int altura = 2;
        final BufferedImage origem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < largura; x++) {
            for (int y = 0; y < altura; y++) {
                origem.setRGB(x, y, 0x10_00_00 * (x + 1) + 0x01_00 * (y + 1) + 7);
            }
        }

        // Para cada valor, onde cai o pixel (x, y) da imagem como o sensor gravou.
        final BiFunction<Integer, int[], int[]> destino = (valor, p) -> switch (valor) {
            case 2 -> new int[] {largura - 1 - p[0], p[1]};
            case 3 -> new int[] {largura - 1 - p[0], altura - 1 - p[1]};
            case 4 -> new int[] {p[0], altura - 1 - p[1]};
            case 5 -> new int[] {p[1], p[0]};
            case 6 -> new int[] {altura - 1 - p[1], p[0]};
            case 7 -> new int[] {altura - 1 - p[1], largura - 1 - p[0]};
            default -> new int[] {p[1], largura - 1 - p[0]};
        };

        for (int valor = 2; valor <= 8; valor++) {
            final BufferedImage girada = OrientacaoExif.aplicar(origem, valor);

            assertThat(girada.getWidth()).as("largura no valor %d", valor).isEqualTo(valor >= 5 ? altura : largura);
            assertThat(girada.getHeight()).as("altura no valor %d", valor).isEqualTo(valor >= 5 ? largura : altura);

            for (int x = 0; x < largura; x++) {
                for (int y = 0; y < altura; y++) {
                    final int[] onde = destino.apply(valor, new int[] {x, y});

                    assertThat(girada.getRGB(onde[0], onde[1]) & 0xFFFFFF)
                            .as("valor %d, pixel (%d, %d) -> (%d, %d)", valor, x, y, onde[0], onde[1])
                            .isEqualTo(origem.getRGB(x, y) & 0xFFFFFF);
                }
            }
        }
    }

    @Test
    @DisplayName("sem rotacao devolve a propria imagem, sem copia")
    void semRotacaoNaoCopia() {
        final BufferedImage imagem = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);

        assertThat(OrientacaoExif.aplicar(imagem, 1)).isSameAs(imagem);
        assertThat(OrientacaoExif.aplicar(imagem, 0)).isSameAs(imagem);
        assertThat(OrientacaoExif.aplicar(imagem, 9)).isSameAs(imagem);
    }
}
