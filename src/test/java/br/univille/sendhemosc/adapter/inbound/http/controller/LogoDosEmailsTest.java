package br.univille.sendhemosc.adapter.inbound.http.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.univille.sendhemosc.FotosDeTeste;
import br.univille.sendhemosc.config.SendHemoscProperties;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * O logo que aparece no topo dos e-mails. A mensagem e aberta fora do sistema, por gente que nao
 * esta logada, entao a imagem tem de ser publica; e como e um PNG com cantos transparentes, um
 * arquivo trocado por engano (JPEG, SVG, tamanho errado) apareceria quebrado ou com fundo preto.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Logo dos e-mails")
class LogoDosEmailsTest {

    private static final String CAMINHO = "/img/email/logo.png";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SendHemoscProperties properties;

    @Test
    @DisplayName("e servido sem login, como PNG")
    void ePublico() throws Exception {
        mockMvc.perform(get(CAMINHO))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
    }

    @Test
    @DisplayName("e um PNG de 144 x 144 com os cantos transparentes e o desenho no meio")
    void eUmPngComTransparencia() throws Exception {
        final byte[] bytes = mockMvc.perform(get(CAMINHO)).andReturn().getResponse().getContentAsByteArray();
        final BufferedImage logo = FotosDeTeste.ler(bytes);

        assertThat(logo.getWidth()).isEqualTo(144);
        assertThat(logo.getHeight()).isEqualTo(144);
        assertThat(logo.getRGB(0, 0) >>> 24).as("canto superior esquerdo, transparente").isZero();
        assertThat(logo.getRGB(143, 143) >>> 24).as("canto inferior direito, transparente").isZero();
        assertThat(logo.getRGB(72, 72) >>> 24).as("o centro tem desenho").isEqualTo(255);
    }

    @Test
    @DisplayName("o endereco que vai nos e-mails aponta para este arquivo")
    void enderecoBateComOArquivo() {
        assertThat(properties.notificacao().urlDoLogo()).isEqualTo(properties.notificacao().urlBase() + CAMINHO);
    }

    @Test
    @DisplayName("a ilustracao do e-mail de convocacao tambem e publica, e e um PNG de 1200 x 340 sem transparencia")
    void ilustracaoDaConvocacao() throws Exception {
        final String caminho = "/img/email/banner.png";
        final byte[] bytes = mockMvc.perform(get(caminho))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andReturn().getResponse().getContentAsByteArray();
        final BufferedImage banner = FotosDeTeste.ler(bytes);

        assertThat(banner.getWidth()).isEqualTo(1200);
        assertThat(banner.getHeight()).isEqualTo(340);
        assertThat(banner.getRGB(600, 5) >>> 24).as("opaca: o fundo azul faz parte da imagem").isEqualTo(255);
        assertThat(properties.notificacao().urlDaImagemDeEmail("banner.png"))
                .isEqualTo(properties.notificacao().urlBase() + caminho);
    }
}
