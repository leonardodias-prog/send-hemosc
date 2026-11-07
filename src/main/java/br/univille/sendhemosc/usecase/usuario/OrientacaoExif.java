package br.univille.sendhemosc.usecase.usuario;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * Orientacao gravada pela camera dentro da foto JPEG (etiqueta EXIF 0x0112).
 *
 * <p>O celular grava a foto sempre na posicao do sensor e anota ali como ela deve ser girada
 * para ser vista de pe. O ImageIO ignora a anotacao, e como a foto e recodificada sem ela, uma
 * selfie tirada na vertical apareceria deitada. Por isso a anotacao e lida aqui e aplicada
 * antes de a imagem ser reduzida.</p>
 *
 * <p>O arquivo vem de fora e pode estar corrompido de proposito: toda leitura confere os
 * limites, e qualquer duvida devolve "sem rotacao" em vez de falhar.</p>
 */
final class OrientacaoExif {

    static final int SEM_ROTACAO = 1;

    private static final int MARCADOR_INICIO = 0xD8;
    private static final int MARCADOR_FIM = 0xD9;
    private static final int MARCADOR_DADOS = 0xDA;
    private static final int MARCADOR_APLICACAO_1 = 0xE1;
    private static final int ETIQUETA_ORIENTACAO = 0x0112;
    private static final byte[] CABECALHO_EXIF = {'E', 'x', 'i', 'f', 0, 0};
    private static final int TAMANHO_ENTRADA = 12;

    private OrientacaoExif() {
    }

    /**
     * Le a orientacao de uma foto JPEG.
     *
     * @param jpeg bytes do arquivo
     * @return de 1 a 8, ou 1 quando a foto nao traz a anotacao ou ela nao faz sentido
     */
    static int ler(final byte[] jpeg) {
        if (jpeg.length < 4 || semSinal(jpeg[0]) != 0xFF || semSinal(jpeg[1]) != MARCADOR_INICIO) {
            return SEM_ROTACAO;
        }

        int posicao = 2;
        while (posicao + 4 <= jpeg.length && semSinal(jpeg[posicao]) == 0xFF) {
            final int marcador = semSinal(jpeg[posicao + 1]);

            if (marcador == 0xFF) {
                // Bytes de preenchimento entre segmentos.
                posicao++;
                continue;
            }
            if (marcador == MARCADOR_DADOS || marcador == MARCADOR_FIM) {
                // Acabaram os cabecalhos: dali em diante e a imagem em si.
                return SEM_ROTACAO;
            }

            final int tamanho = (semSinal(jpeg[posicao + 2]) << 8) | semSinal(jpeg[posicao + 3]);
            final int fim = posicao + 2 + tamanho;
            if (tamanho < 2 || fim > jpeg.length) {
                return SEM_ROTACAO;
            }
            if (marcador == MARCADOR_APLICACAO_1 && comecaComExif(jpeg, posicao + 4, fim)) {
                return orientacaoDoTiff(jpeg, posicao + 4 + CABECALHO_EXIF.length, fim);
            }

            posicao = fim;
        }

        return SEM_ROTACAO;
    }

    /**
     * Gira ou espelha a imagem para que fique de pe.
     *
     * @param imagem imagem como o sensor gravou
     * @param orientacao valor lido por {@link #ler(byte[])}
     * @return a imagem na posicao certa, a mesma instancia quando nao ha o que fazer
     */
    static BufferedImage aplicar(final BufferedImage imagem, final int orientacao) {
        if (orientacao <= SEM_ROTACAO || orientacao > 8) {
            return imagem;
        }

        final int largura = imagem.getWidth();
        final int altura = imagem.getHeight();
        final boolean trocaOsLados = orientacao >= 5;
        final BufferedImage girada = new BufferedImage(trocaOsLados ? altura : largura,
                trocaOsLados ? largura : altura, BufferedImage.TYPE_INT_RGB);
        final Graphics2D desenho = girada.createGraphics();

        try {
            desenho.drawImage(imagem, transformacao(orientacao, largura, altura), null);
        } finally {
            desenho.dispose();
        }

        return girada;
    }

    /**
     * Matriz de cada orientacao, na ordem do AffineTransform: m00, m10, m01, m11, m02, m12, onde
     * x' = m00*x + m01*y + m02 e y' = m10*x + m11*y + m12.
     */
    private static AffineTransform transformacao(final int orientacao, final int largura, final int altura) {
        return switch (orientacao) {
            case 2 -> new AffineTransform(-1, 0, 0, 1, largura, 0);
            case 3 -> new AffineTransform(-1, 0, 0, -1, largura, altura);
            case 4 -> new AffineTransform(1, 0, 0, -1, 0, altura);
            case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);
            case 6 -> new AffineTransform(0, 1, -1, 0, altura, 0);
            case 7 -> new AffineTransform(0, -1, -1, 0, altura, largura);
            default -> new AffineTransform(0, -1, 1, 0, 0, largura);
        };
    }

    private static boolean comecaComExif(final byte[] dados, final int inicio, final int fim) {
        if (inicio + CABECALHO_EXIF.length > fim) {
            return false;
        }
        for (int i = 0; i < CABECALHO_EXIF.length; i++) {
            if (dados[inicio + i] != CABECALHO_EXIF[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Procura a etiqueta de orientacao na primeira tabela (IFD0) do bloco TIFF. Os enderecos
     * dentro do bloco contam a partir do inicio dele.
     */
    private static int orientacaoDoTiff(final byte[] dados, final int tiff, final int fim) {
        if (tiff + 8 > fim) {
            return SEM_ROTACAO;
        }

        final boolean pequenoPrimeiro = dados[tiff] == 'I' && dados[tiff + 1] == 'I';
        final boolean grandePrimeiro = dados[tiff] == 'M' && dados[tiff + 1] == 'M';
        if (!pequenoPrimeiro && !grandePrimeiro) {
            return SEM_ROTACAO;
        }

        final long enderecoDaTabela = inteiro(dados, tiff + 4, pequenoPrimeiro, fim);
        if (enderecoDaTabela < 8 || tiff + enderecoDaTabela + 2 > fim) {
            return SEM_ROTACAO;
        }

        final int tabela = (int) (tiff + enderecoDaTabela);
        final int entradas = curto(dados, tabela, pequenoPrimeiro, fim);

        for (int i = 0; i < entradas; i++) {
            final int entrada = tabela + 2 + i * TAMANHO_ENTRADA;
            if (entrada + TAMANHO_ENTRADA > fim) {
                return SEM_ROTACAO;
            }
            if (curto(dados, entrada, pequenoPrimeiro, fim) == ETIQUETA_ORIENTACAO) {
                final int valor = curto(dados, entrada + 8, pequenoPrimeiro, fim);

                return valor >= 1 && valor <= 8 ? valor : SEM_ROTACAO;
            }
        }

        return SEM_ROTACAO;
    }

    private static int curto(final byte[] dados, final int posicao, final boolean pequenoPrimeiro, final int fim) {
        if (posicao < 0 || posicao + 2 > fim) {
            return -1;
        }

        return pequenoPrimeiro
                ? semSinal(dados[posicao]) | (semSinal(dados[posicao + 1]) << 8)
                : (semSinal(dados[posicao]) << 8) | semSinal(dados[posicao + 1]);
    }

    private static long inteiro(final byte[] dados, final int posicao, final boolean pequenoPrimeiro, final int fim) {
        if (posicao < 0 || posicao + 4 > fim) {
            return -1;
        }

        final long baixo = pequenoPrimeiro ? curto(dados, posicao, true, fim) : curto(dados, posicao + 2, false, fim);
        final long alto = pequenoPrimeiro ? curto(dados, posicao + 2, true, fim) : curto(dados, posicao, false, fim);

        return (alto << 16) | baixo;
    }

    private static int semSinal(final byte valor) {
        return valor & 0xFF;
    }
}
