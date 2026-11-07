package br.univille.sendhemosc.usecase.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.univille.sendhemosc.FotosDeTeste;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.exception.UsuarioErrorsMessage;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Preparo da foto de perfil")
class ProcessadorDeFotoTest {

    private static void recusa(final byte[] arquivo, final UsuarioErrorsMessage motivo) {
        assertThatThrownBy(() -> ProcessadorDeFoto.processar(arquivo))
                .isInstanceOfSatisfying(NegocioException.class,
                        excecao -> assertThat(excecao.getErro()).isEqualTo(motivo));
    }

    private static boolean contem(final byte[] bytes, final String texto) {
        return new String(bytes, StandardCharsets.ISO_8859_1).contains(texto);
    }

    @Test
    @DisplayName("foto grande vira um JPEG quadrado de 256 x 256")
    void fotoGrandeViraQuadradoDe256() {
        final byte[] saida = ProcessadorDeFoto.processar(FotosDeTeste.jpeg(3000, 2000));
        final BufferedImage lida = FotosDeTeste.ler(saida);

        assertThat(FotosDeTeste.eJpeg(saida)).isTrue();
        assertThat(lida.getWidth()).isEqualTo(256);
        assertThat(lida.getHeight()).isEqualTo(256);
        assertThat(saida.length).as("poucas dezenas de KB, nao os megabytes do original").isLessThan(60_000);
    }

    @Test
    @DisplayName("foto pequena nao e ampliada: ficaria borrada")
    void fotoPequenaNaoEAmpliada() {
        final BufferedImage lida = FotosDeTeste.ler(ProcessadorDeFoto.processar(FotosDeTeste.jpeg(100, 60)));

        assertThat(lida.getWidth()).isEqualTo(60);
        assertThat(lida.getHeight()).isEqualTo(60);
    }

    @Test
    @DisplayName("o recorte e o quadrado do centro")
    void recorteECentral() {
        // 400 x 100: o centro de 100 x 100 pega 150 pixels de cada lado fora, entao so sobra o
        // trecho em volta da divisa vermelho/azul.
        final BufferedImage lida = FotosDeTeste.ler(ProcessadorDeFoto.processar(FotosDeTeste.jpeg(400, 100)));
        final Color esquerda = new Color(lida.getRGB(10, 50));
        final Color direita = new Color(lida.getRGB(lida.getWidth() - 10, 50));

        assertThat(lida.getWidth()).isEqualTo(100);
        assertThat(esquerda.getRed()).isGreaterThan(esquerda.getBlue());
        assertThat(direita.getBlue()).isGreaterThan(direita.getRed());
    }

    @Test
    @DisplayName("PNG e GIF sao aceitos e saem como JPEG")
    void pngEGifSaoAceitos() {
        assertThat(FotosDeTeste.eJpeg(ProcessadorDeFoto.processar(FotosDeTeste.png(500, 500)))).isTrue();
        assertThat(FotosDeTeste.eJpeg(ProcessadorDeFoto.processar(FotosDeTeste.gif(500, 500)))).isTrue();
    }

    @Test
    @DisplayName("o que era transparente vira branco, e nao preto")
    void transparenciaViraBranco() {
        final BufferedImage lida = FotosDeTeste.ler(ProcessadorDeFoto.processar(FotosDeTeste.pngTransparente(80)));
        final Color centro = new Color(lida.getRGB(40, 40));

        assertThat(centro.getRed()).isGreaterThan(240);
        assertThat(centro.getGreen()).isGreaterThan(240);
        assertThat(centro.getBlue()).isGreaterThan(240);
    }

    @Test
    @DisplayName("o que viajava junto com a foto (EXIF, onde ficam GPS e modelo da camera) nao sai")
    void metadadosNaoSaem() {
        final byte[] comExif = FotosDeTeste.jpegComOrientacao(800, 600, 3, true);

        assertThat(contem(comExif, "Exif")).as("o arquivo de teste tem EXIF").isTrue();

        final byte[] saida = ProcessadorDeFoto.processar(comExif);

        assertThat(contem(saida, "Exif")).isFalse();
        assertThat(OrientacaoExif.ler(saida)).isEqualTo(OrientacaoExif.SEM_ROTACAO);
    }

    @Test
    @DisplayName("a foto de celular tirada em pe (orientacao 6) sai em pe")
    void fotoDeCelularSaiEmPe() {
        // Esquerda vermelha e direita azul, com a anotacao "gire 90 graus": depois de girar, o
        // vermelho fica em cima e o azul embaixo.
        final BufferedImage lida = FotosDeTeste.ler(
                ProcessadorDeFoto.processar(FotosDeTeste.jpegComOrientacao(800, 600, 6, true)));
        final Color cima = new Color(lida.getRGB(lida.getWidth() / 2, 20));
        final Color baixo = new Color(lida.getRGB(lida.getWidth() / 2, lida.getHeight() - 20));

        assertThat(cima.getRed()).as("em cima").isGreaterThan(cima.getBlue());
        assertThat(baixo.getBlue()).as("embaixo").isGreaterThan(baixo.getRed());
    }

    @Test
    @DisplayName("orientacao 3 (de cabeca para baixo) inverte esquerda e direita")
    void fotoDeCabecaParaBaixo() {
        final BufferedImage lida = FotosDeTeste.ler(
                ProcessadorDeFoto.processar(FotosDeTeste.jpegComOrientacao(600, 600, 3, false)));
        final Color esquerda = new Color(lida.getRGB(20, lida.getHeight() / 2));
        final Color direita = new Color(lida.getRGB(lida.getWidth() - 20, lida.getHeight() / 2));

        assertThat(esquerda.getBlue()).isGreaterThan(esquerda.getRed());
        assertThat(direita.getRed()).isGreaterThan(direita.getBlue());
    }

    @Test
    @DisplayName("texto com nome de imagem nao e imagem")
    void textoNaoEImagem() {
        recusa("isto e so um texto, mesmo que a pessoa o chame de foto.jpg".getBytes(StandardCharsets.UTF_8),
                UsuarioErrorsMessage.FOTO_INVALIDA);
    }

    @Test
    @DisplayName("SVG e recusado: pode carregar script, e nao e imagem raster")
    void svgERecusado() {
        recusa("<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"
                .getBytes(StandardCharsets.UTF_8), UsuarioErrorsMessage.FOTO_INVALIDA);
    }

    @Test
    @DisplayName("formato que o ImageIO le mas a lista nao inclui (BMP) e recusado")
    void formatoForaDaListaERecusado() {
        recusa(FotosDeTeste.bmp(50, 50), UsuarioErrorsMessage.FOTO_INVALIDA);
    }

    @Test
    @DisplayName("cabecalho de JPEG seguido de lixo e recusado, sem estourar excecao")
    void cabecalhoDeJpegComLixo() {
        final byte[] falso = new byte[500];
        new Random(3).nextBytes(falso);
        falso[0] = (byte) 0xFF;
        falso[1] = (byte) 0xD8;
        falso[2] = (byte) 0xFF;

        recusa(falso, UsuarioErrorsMessage.FOTO_INVALIDA);
    }

    @Test
    @DisplayName("imagem cortada no meio e recusada")
    void imagemCortada() {
        final byte[] inteira = FotosDeTeste.png(300, 300);

        recusa(java.util.Arrays.copyOf(inteira, inteira.length / 3), UsuarioErrorsMessage.FOTO_INVALIDA);
    }

    @Test
    @DisplayName("arquivo vazio ou ausente e recusado")
    void vazioERecusado() {
        recusa(new byte[0], UsuarioErrorsMessage.FOTO_INVALIDA);
        recusa(null, UsuarioErrorsMessage.FOTO_INVALIDA);
    }

    @Test
    @DisplayName("acima de 8 MB e grande demais, sem nem tentar decodificar")
    void acimaDoLimiteDeBytes() {
        recusa(new byte[ProcessadorDeFoto.TAMANHO_MAXIMO_EM_BYTES + 1], UsuarioErrorsMessage.FOTO_GRANDE_DEMAIS);
    }

    @Test
    @DisplayName("pixels demais sao recusados pelo cabecalho: um PNG pequeno pode declarar bilhoes")
    void cabecalhoQueDeclaraPixelsDemais() {
        final byte[] mentiroso = FotosDeTeste.pngComDimensoesDeclaradas(FotosDeTeste.png(10, 10), 100_000, 100_000);

        recusa(mentiroso, UsuarioErrorsMessage.FOTO_INVALIDA);
    }

    @Test
    @DisplayName("imagem de 49 milhoes de pixels, de verdade, passa do teto e e recusada")
    void acimaDoTetoDePixels() {
        recusa(FotosDeTeste.pngCinza(7000, 7000), UsuarioErrorsMessage.FOTO_INVALIDA);
    }

    @Test
    @DisplayName("imagem de 36 milhoes de pixels e aceita, lida ja reduzida")
    void fotoDeTrintaESeisMegapixelsPassa() {
        final BufferedImage lida = FotosDeTeste.ler(ProcessadorDeFoto.processar(FotosDeTeste.pngCinza(6000, 6000)));

        assertThat(lida.getWidth()).isEqualTo(256);
        assertThat(lida.getHeight()).isEqualTo(256);
    }
}
