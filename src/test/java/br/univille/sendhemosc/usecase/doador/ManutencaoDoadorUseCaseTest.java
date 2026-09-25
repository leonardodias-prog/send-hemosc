package br.univille.sendhemosc.usecase.doador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.univille.sendhemosc.domain.dto.DadosDoTitular;
import br.univille.sendhemosc.domain.dto.DoadorCadastrado;
import br.univille.sendhemosc.domain.dto.NovoDoador;
import br.univille.sendhemosc.domain.enums.Sexo;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.exception.DoadorErrorsMessage;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import br.univille.sendhemosc.domain.port.outbound.IConsentimentoPort;
import br.univille.sendhemosc.domain.port.outbound.IDadosDoTitularPort;
import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Cobre o que faltava no cadastro de doador: editar, desativar, excluir e exportar os dados.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Manutencao do cadastro de doador")
class ManutencaoDoadorUseCaseTest {

    private static final Long ID = 7L;

    @Mock
    private IDoadorRepositoryPort doadorRepository;
    @Mock
    private IConsentimentoPort consentimento;
    @Mock
    private IAuditoriaPort auditoria;
    @Mock
    private IDadosDoTitularPort dadosDoTitular;

    private static DoadorCadastrado doador(final boolean aceitaContato) {
        return new DoadorCadastrado(ID, "Beatriz Souza", "beatriz@example.org", null, TipoSanguineo.O_NEGATIVO,
                Sexo.FEMININO, LocalDate.of(1990, 5, 10), null, aceitaContato, true,
                LocalDateTime.of(2026, 1, 10, 9, 0), LocalDateTime.of(2026, 1, 10, 9, 0));
    }

    private static NovoDoador formulario(final String email, final boolean aceitaContato) {
        return new NovoDoador("  Beatriz Souza Lima ", email, null, TipoSanguineo.O_NEGATIVO, Sexo.FEMININO,
                LocalDate.of(1990, 5, 10), null, aceitaContato);
    }

    @Nested
    @DisplayName("edicao")
    class Edicao {

        @Test
        @DisplayName("normaliza nome e e-mail como o cadastro")
        void normaliza() {
            when(doadorRepository.buscarPorId(ID)).thenReturn(Optional.of(doador(true)));

            new EditarDoadorUseCase(doadorRepository, consentimento).execute(ID, formulario(" Bia@Example.org ", true));

            final ArgumentCaptor<NovoDoador> gravado = ArgumentCaptor.forClass(NovoDoador.class);
            verify(doadorRepository).atualizar(eq(ID), gravado.capture());
            assertThat(gravado.getValue().nome()).isEqualTo("Beatriz Souza Lima");
            assertThat(gravado.getValue().email()).isEqualTo("bia@example.org");
        }

        @Test
        @DisplayName("recusa e-mail que ja pertence a outro doador")
        void recusaEmailDeOutro() {
            when(doadorRepository.buscarPorId(ID)).thenReturn(Optional.of(doador(true)));
            when(doadorRepository.existeComEmailEmOutro("outro@example.org", ID)).thenReturn(true);

            assertThatThrownBy(() -> new EditarDoadorUseCase(doadorRepository, consentimento)
                    .execute(ID, formulario("outro@example.org", true)))
                    .isInstanceOfSatisfying(NegocioException.class,
                            excecao -> assertThat(excecao.getErro()).isEqualTo(DoadorErrorsMessage.EMAIL_DUPLICADO));

            verify(doadorRepository, never()).atualizar(anyLong(), any());
        }

        @Test
        @DisplayName("mudar a autorizacao de contato fica no historico de consentimento")
        void registraMudancaDeConsentimento() {
            when(doadorRepository.buscarPorId(ID)).thenReturn(Optional.of(doador(true)));

            new EditarDoadorUseCase(doadorRepository, consentimento).execute(ID, formulario("beatriz@example.org", false));

            verify(consentimento).registrar(ID, false, "EDICAO");
        }

        @Test
        @DisplayName("editar outro campo nao gera registro de consentimento")
        void semMudancaDeConsentimento() {
            when(doadorRepository.buscarPorId(ID)).thenReturn(Optional.of(doador(true)));

            new EditarDoadorUseCase(doadorRepository, consentimento).execute(ID, formulario("beatriz@example.org", true));

            verify(consentimento, never()).registrar(anyLong(), anyBoolean(), anyString());
        }
    }

    @Nested
    @DisplayName("exclusao")
    class Exclusao {

        @Test
        @DisplayName("a pedido do titular, a auditoria nao guarda nome nem e-mail")
        void auditoriaSemDadosPessoais() {
            when(doadorRepository.buscarPorToken("tok")).thenReturn(Optional.of(doador(true)));

            new ExcluirDoadorUseCase(doadorRepository, auditoria).porToken("tok");

            verify(doadorRepository).excluir(ID);
            final ArgumentCaptor<String> detalhe = ArgumentCaptor.forClass(String.class);
            verify(auditoria).registrar(eq("DOADOR_EXCLUIDO"), detalhe.capture());
            assertThat(detalhe.getValue()).contains("TITULAR").doesNotContain("Beatriz", "beatriz@example.org");
        }

        @Test
        @DisplayName("token desconhecido nao exclui nada")
        void tokenDesconhecido() {
            when(doadorRepository.buscarPorToken("nada")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> new ExcluirDoadorUseCase(doadorRepository, auditoria).porToken("nada"))
                    .isInstanceOf(NegocioException.class);

            verify(doadorRepository, never()).excluir(anyLong());
        }
    }

    @Test
    @DisplayName("exportacao reune cadastro e historico, e fica em auditoria")
    void exporta() {
        when(doadorRepository.buscarPorId(ID)).thenReturn(Optional.of(doador(true)));
        when(dadosDoTitular.consentimentos(ID)).thenReturn(List.of(
                new DadosDoTitular.Consentimento("1.0", true, "CADASTRO", LocalDateTime.of(2026, 1, 10, 9, 0))));
        when(dadosDoTitular.doacoes(ID)).thenReturn(List.of(
                new DadosDoTitular.Doacao(LocalDate.of(2026, 3, 1), "Joinville", null)));
        when(dadosDoTitular.convocacoes(ID)).thenReturn(List.of());

        final DadosDoTitular dados = new ExportarDadosDoTitularUseCase(doadorRepository, dadosDoTitular, auditoria)
                .porId(ID);

        assertThat(dados.cadastro().email()).isEqualTo("beatriz@example.org");
        assertThat(dados.cadastro().tipoSanguineo()).isEqualTo("O-");
        assertThat(dados.consentimentos()).hasSize(1);
        assertThat(dados.doacoes()).hasSize(1);
        verify(auditoria).registrar(eq("DADOS_DO_TITULAR_EXPORTADOS"), anyString());
    }

    @Test
    @DisplayName("desativar registra em auditoria")
    void desativa() {
        when(doadorRepository.buscarPorId(ID)).thenReturn(Optional.of(doador(true)));

        final String nome = new AlterarSituacaoDoadorUseCase(doadorRepository, auditoria).execute(ID, false);

        assertThat(nome).isEqualTo("Beatriz Souza");
        verify(doadorRepository).definirAtivo(ID, false);
        verify(auditoria).registrar(eq("DOADOR_DESATIVADO"), anyString());
    }
}
