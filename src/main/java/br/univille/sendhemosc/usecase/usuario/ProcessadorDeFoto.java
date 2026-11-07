package br.univille.sendhemosc.usecase.usuario;

import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.exception.UsuarioErrorsMessage;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/**
 * Transforma o arquivo que a pessoa enviou na foto que o sistema guarda: um quadrado de ate
 * 256 x 256, em JPEG.
 *
 * <p>Nada do arquivo original e aproveitado. Ele e decodificado e desenhado de novo, o que
 * garante tres coisas de uma vez: so entra o que for imagem de verdade (o tipo declarado pelo
 * navegador e o nome do arquivo nao valem nada, so o conteudo), o que viaja junto com a imagem
 * (localizacao do GPS, modelo da camera, miniatura) fica para tras, e o banco guarda dezenas de
 * KB em vez dos megabytes de uma foto de celular.</p>
 *
 * <p>Imagem muito grande e recusada antes de ser decodificada, pelas dimensoes lidas no
 * cabecalho: um PNG de poucos KB pode declarar bilhoes de pixels. As que passam sao lidas ja
 * reduzidas (amostragem na propria decodificacao), para que uma foto de 12 megapixels nao ocupe
 * dezenas de MB de memoria num servico de 512 MB.</p>
 */
final class ProcessadorDeFoto {

    static final String TIPO_GRAVADO = "image/jpeg";
    static final int LADO_MAXIMO = 256;
    static final int TAMANHO_MAXIMO_EM_BYTES = 8 * 1024 * 1024;

    private static final long PIXELS_MAXIMOS = 40_000_000L;
    private static final float QUALIDADE_DO_JPEG = 0.85f;
    private static final Set<String> FORMATOS_ACEITOS = Set.of("jpeg", "png", "gif");

    private ProcessadorDeFoto() {
    }

    /**
     * Prepara a foto para ser guardada.
     *
     * @param original bytes do arquivo enviado
     * @return a foto reduzida, em JPEG
     * @throws NegocioException quando o arquivo e grande demais ou nao e uma imagem aceita
     */
    static byte[] processar(final byte[] original) {
        if (original == null || original.length == 0) {
            throw new NegocioException(UsuarioErrorsMessage.FOTO_INVALIDA);
        }
        if (original.length > TAMANHO_MAXIMO_EM_BYTES) {
            throw new NegocioException(UsuarioErrorsMessage.FOTO_GRANDE_DEMAIS);
        }

        try (ImageInputStream entrada = new MemoryCacheImageInputStream(new ByteArrayInputStream(original))) {
            final ImageReader leitor = escolherLeitor(entrada);

            try {
                leitor.setInput(entrada, false, true);

                final BufferedImage lida = ler(leitor);
                final int orientacao = "jpeg".equals(nomeDoFormato(leitor))
                        ? OrientacaoExif.ler(original) : OrientacaoExif.SEM_ROTACAO;

                return codificar(reduzir(OrientacaoExif.aplicar(lida, orientacao)));
            } finally {
                leitor.dispose();
            }
        } catch (final NegocioException recusa) {
            throw recusa;
        } catch (final IOException | RuntimeException excecao) {
            // Arquivo corrompido ou de um subtipo que o ImageIO nao le (JPEG em CMYK, por exemplo).
            throw new NegocioException(UsuarioErrorsMessage.FOTO_INVALIDA, excecao);
        }
    }

    private static ImageReader escolherLeitor(final ImageInputStream entrada) throws IOException {
        final Iterator<ImageReader> candidatos = ImageIO.getImageReaders(entrada);

        while (candidatos.hasNext()) {
            final ImageReader candidato = candidatos.next();

            if (FORMATOS_ACEITOS.contains(nomeDoFormato(candidato))) {
                return candidato;
            }
            candidato.dispose();
        }

        throw new NegocioException(UsuarioErrorsMessage.FOTO_INVALIDA);
    }

    private static String nomeDoFormato(final ImageReader leitor) {
        try {
            return leitor.getFormatName().toLowerCase(Locale.ROOT);
        } catch (final IOException excecao) {
            return "";
        }
    }

    private static BufferedImage ler(final ImageReader leitor) throws IOException {
        final int largura = leitor.getWidth(0);
        final int altura = leitor.getHeight(0);

        if (largura <= 0 || altura <= 0 || (long) largura * altura > PIXELS_MAXIMOS) {
            throw new NegocioException(UsuarioErrorsMessage.FOTO_INVALIDA);
        }

        // Pula pixels ao decodificar, de modo que o lado menor fique entre 2 e 4 vezes o final:
        // sobra resolucao para a reducao fina e a memoria nao depende do tamanho da foto.
        final int salto = Math.max(1, Math.min(largura, altura) / (LADO_MAXIMO * 2));
        final ImageReadParam parametros = leitor.getDefaultReadParam();
        parametros.setSourceSubsampling(salto, salto, 0, 0);

        return leitor.read(0, parametros);
    }

    /**
     * Recorta o quadrado central e reduz ate o tamanho final, nunca ampliando: foto pequena
     * continua pequena em vez de ficar borrada. A reducao e feita em passos de metade e so o
     * ultimo e fracionario, que e o que da um resultado limpo sem filtro especial.
     */
    private static BufferedImage reduzir(final BufferedImage imagem) {
        final int lado = Math.min(imagem.getWidth(), imagem.getHeight());
        final int destino = Math.min(LADO_MAXIMO, lado);
        final int esquerda = (imagem.getWidth() - lado) / 2;
        final int topo = (imagem.getHeight() - lado) / 2;

        BufferedImage atual = imagem.getSubimage(esquerda, topo, lado, lado);
        while (atual.getWidth() >= destino * 2) {
            atual = redesenhar(atual, atual.getWidth() / 2, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        }

        return redesenhar(atual, destino, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    /**
     * Desenha sobre fundo branco: o JPEG nao tem transparencia, e o que era transparente num PNG
     * ou GIF viraria preto.
     */
    private static BufferedImage redesenhar(final BufferedImage origem, final int lado, final Object interpolacao) {
        final BufferedImage saida = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_RGB);
        final Graphics2D desenho = saida.createGraphics();

        try {
            desenho.setColor(Color.WHITE);
            desenho.fillRect(0, 0, lado, lado);
            desenho.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolacao);
            desenho.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            desenho.drawImage(origem, 0, 0, lado, lado, null);
        } finally {
            desenho.dispose();
        }

        return saida;
    }

    private static byte[] codificar(final BufferedImage imagem) throws IOException {
        final ImageWriter escritor = ImageIO.getImageWritersByFormatName("jpeg").next();

        try (ByteArrayOutputStream saida = new ByteArrayOutputStream();
                ImageOutputStream destino = new MemoryCacheImageOutputStream(saida)) {
            final ImageWriteParam parametros = escritor.getDefaultWriteParam();
            parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parametros.setCompressionQuality(QUALIDADE_DO_JPEG);

            escritor.setOutput(destino);
            escritor.write(null, new IIOImage(imagem, null, null), parametros);
            destino.flush();

            return saida.toByteArray();
        } finally {
            escritor.dispose();
        }
    }
}
