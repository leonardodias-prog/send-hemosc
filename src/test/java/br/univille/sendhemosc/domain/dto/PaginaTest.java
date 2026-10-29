package br.univille.sendhemosc.domain.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Pagina")
class PaginaTest {

    private static final List<Integer> SESSENTA = IntStream.rangeClosed(1, 60).boxed().toList();

    @Test
    @DisplayName("recorta o trecho pedido e informa a posicao dele na lista")
    void recortaTrecho() {
        final Pagina<Integer> pagina = Pagina.recortar(SESSENTA, 2, 25);

        assertThat(pagina.itens()).first().isEqualTo(26);
        assertThat(pagina.itens()).hasSize(25);
        assertThat(pagina.totalPaginas()).isEqualTo(3);
        assertThat(pagina.primeiroItem()).isEqualTo(26);
        assertThat(pagina.ultimoItem()).isEqualTo(50);
        assertThat(pagina.temAnterior()).isTrue();
        assertThat(pagina.temProxima()).isTrue();
    }

    @Test
    @DisplayName("pagina alem do fim cai na ultima, e abaixo de 1 cai na primeira")
    void foraDoIntervalo() {
        assertThat(Pagina.recortar(SESSENTA, 9, 25).numero()).isEqualTo(3);
        assertThat(Pagina.recortar(SESSENTA, 9, 25).itens()).hasSize(10);
        assertThat(Pagina.recortar(SESSENTA, 0, 25).numero()).isEqualTo(1);
    }

    @Test
    @DisplayName("lista vazia e uma pagina sem itens")
    void listaVazia() {
        final Pagina<Integer> pagina = Pagina.recortar(List.of(), 1, 25);

        assertThat(pagina.totalPaginas()).isEqualTo(1);
        assertThat(pagina.itens()).isEmpty();
        assertThat(pagina.temProxima()).isFalse();
    }
}
