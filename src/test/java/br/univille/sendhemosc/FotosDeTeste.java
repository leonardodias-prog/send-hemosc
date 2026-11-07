package br.univille.sendhemosc;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;

/**
 * Fabrica imagens para os testes da foto de perfil. Tudo e gerado na hora: nenhum arquivo de
 * imagem fica versionado.
 */
public final class FotosDeTeste {

    private FotosDeTeste() {
    }

    /** JPEG com a metade esquerda vermelha e a direita azul, de modo que girar a imagem se note. */
    public static byte[] jpeg(final int largura, final int altura) {
        return escrever(duasMetades(largura, altura), "jpg");
    }

    public static byte[] png(final int largura, final int altura) {
        return escrever(duasMetades(largura, altura), "png");
    }

    public static byte[] gif(final int largura, final int altura) {
        return escrever(duasMetades(largura, altura), "gif");
    }

    public static byte[] bmp(final int largura, final int altura) {
        return escrever(duasMetades(largura, altura), "bmp");
    }

    /** PNG sem nenhum pixel opaco: o que o fundo do JPEG vai mostrar e a cor de fundo escolhida. */
    public static byte[] pngTransparente(final int lado) {
        return escrever(new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB), "png");
    }

    /** PNG em tons de cinza, que ocupa pouca memoria para um numero enorme de pixels. */
    public static byte[] pngCinza(final int largura, final int altura) {
        return escrever(new BufferedImage(largura, altura, BufferedImage.TYPE_BYTE_GRAY), "png");
    }

    /** O mesmo JPEG de {@link #jpeg(int, int)}, com a anotacao de orientacao que a camera grava. */
    public static byte[] jpegComOrientacao(final int largura, final int altura, final int orientacao,
                                           final boolean pequenoPrimeiro) {
        return comOrientacao(jpeg(largura, altura), orientacao, pequenoPrimeiro);
    }

    /**
     * Insere o bloco EXIF logo depois do marcador de inicio, com uma unica etiqueta: a orientacao.
     */
    public static byte[] comOrientacao(final byte[] jpeg, final int orientacao, final boolean pequenoPrimeiro) {
        final byte letra = (byte) (pequenoPrimeiro ? 'I' : 'M');
        final ByteBuffer tiff = ByteBuffer.allocate(8 + 2 + 12 + 4)
                .order(pequenoPrimeiro ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);

        tiff.put(letra).put(letra);
        tiff.putShort((short) 0x2A);
        tiff.putInt(8);
        tiff.putShort((short) 1);
        tiff.putShort((short) 0x0112);
        tiff.putShort((short) 3);
        tiff.putInt(1);
        tiff.putShort((short) orientacao);
        tiff.putShort((short) 0);
        tiff.putInt(0);

        final byte[] bloco = tiff.array();
        final int tamanho = 2 + 6 + bloco.length;
        final ByteArrayOutputStream saida = new ByteArrayOutputStream();

        saida.write(jpeg, 0, 2);
        saida.write(0xFF);
        saida.write(0xE1);
        saida.write(tamanho >> 8);
        saida.write(tamanho & 0xFF);
        saida.writeBytes("Exif\0\0".getBytes(StandardCharsets.ISO_8859_1));
        saida.writeBytes(bloco);
        saida.write(jpeg, 2, jpeg.length - 2);

        return saida.toByteArray();
    }

    /**
     * Troca a largura e a altura declaradas no cabecalho de um PNG, sem mexer nos dados. O
     * cabecalho passa a mentir sobre o tamanho, como faz o arquivo feito para esgotar a memoria.
     */
    public static byte[] pngComDimensoesDeclaradas(final byte[] png, final int largura, final int altura) {
        final byte[] copia = png.clone();
        final ByteBuffer cabecalho = ByteBuffer.wrap(copia);

        cabecalho.putInt(16, largura);
        cabecalho.putInt(20, altura);

        final CRC32 soma = new CRC32();
        soma.update(copia, 12, 17);
        cabecalho.putInt(29, (int) soma.getValue());

        return copia;
    }

    public static BufferedImage ler(final byte[] imagem) {
        try {
            return ImageIO.read(new ByteArrayInputStream(imagem));
        } catch (final IOException excecao) {
            throw new UncheckedIOException(excecao);
        }
    }

    /** Sentinela do formato JPEG: os dois primeiros bytes de qualquer arquivo. */
    public static boolean eJpeg(final byte[] imagem) {
        return imagem.length > 3 && (imagem[0] & 0xFF) == 0xFF && (imagem[1] & 0xFF) == 0xD8;
    }

    private static BufferedImage duasMetades(final int largura, final int altura) {
        final BufferedImage imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        final Graphics2D desenho = imagem.createGraphics();

        try {
            desenho.setColor(Color.RED);
            desenho.fillRect(0, 0, largura / 2, altura);
            desenho.setColor(Color.BLUE);
            desenho.fillRect(largura / 2, 0, largura - largura / 2, altura);
        } finally {
            desenho.dispose();
        }

        return imagem;
    }

    private static byte[] escrever(final BufferedImage imagem, final String formato) {
        try (ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            ImageIO.write(imagem, formato, saida);

            return saida.toByteArray();
        } catch (final IOException excecao) {
            throw new UncheckedIOException(excecao);
        }
    }
}
