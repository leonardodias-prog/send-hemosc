package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.domain.exception.ErrorsMessage;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.exception.UsuarioErrorsMessage;
import br.univille.sendhemosc.usecase.usuario.GerenciarPerfilUseCase;
import br.univille.sendhemosc.usecase.usuario.TrocarPropriaSenhaUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * O perfil de quem esta autenticado: os dados da conta, a foto e a troca de senha, abertos a todo
 * perfil de acesso. Tudo aqui age sobre a conta da propria sessao; nenhuma rota recebe o
 * identificador de outra conta, de modo que ninguem alcanca o perfil de ninguem.
 *
 * <p>Senha digitada nunca volta para o formulario, nem depois de erro: devolve-la deixaria o
 * valor no HTML da pagina.</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ContaViewAdapter {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final String VOLTAR_AO_PERFIL = "redirect:/conta";

    private final TrocarPropriaSenhaUseCase trocarPropriaSenha;
    private final GerenciarPerfilUseCase gerenciarPerfil;
    private final MessageSource messageSource;

    @GetMapping("/conta")
    public String perfil(final Principal autenticado, final Model model) {
        model.addAttribute("perfil", gerenciarPerfil.consultar(autenticado.getName()));

        return "conta";
    }

    /**
     * A troca de senha morava aqui, numa tela so dela. Quem guardou o endereco cai no perfil,
     * onde ela continua.
     */
    @GetMapping("/conta/senha")
    public String formularioDeSenha() {
        return VOLTAR_AO_PERFIL;
    }

    @PostMapping("/conta/senha")
    public String trocarSenha(@RequestParam(required = false) final String senhaAtual,
                              @RequestParam(required = false) final String novaSenha,
                              @RequestParam(required = false) final String confirmacao,
                              final Principal autenticado,
                              final RedirectAttributes atributos) {
        try {
            trocarPropriaSenha.execute(autenticado.getName(), senhaAtual, novaSenha, confirmacao);
            atributos.addFlashAttribute("aviso", "Senha alterada. Na próxima entrada, use a nova.");
        } catch (final NegocioException excecao) {
            atributos.addFlashAttribute("erro", mensagemDe(excecao.getErro()));
        }

        return VOLTAR_AO_PERFIL;
    }

    /**
     * Corrige nome e e-mail. Trocar o e-mail encerra a sessao: o e-mail e o nome com que a pessoa
     * entrou, e a sessao aberta ainda carregaria o antigo. Ela entra de novo com o endereco novo,
     * o que tambem confirma que ela o digitou certo.
     */
    @PostMapping("/conta/dados")
    public String alterarDados(@RequestParam(required = false) final String nome,
                               @RequestParam(required = false) final String email,
                               @RequestParam(required = false) final String senhaAtual,
                               final Principal autenticado,
                               final Authentication autenticacao,
                               final HttpServletRequest requisicao,
                               final HttpServletResponse resposta,
                               final RedirectAttributes atributos) {
        try {
            final var resultado = gerenciarPerfil.alterarDados(autenticado.getName(), nome, email, senhaAtual);

            if (resultado.emailMudou()) {
                new SecurityContextLogoutHandler().logout(requisicao, resposta, autenticacao);

                // A sessao acabou de ser encerrada, e a mensagem de um redirecionamento mora nela:
                // por isso o aviso vai no endereco.
                return "redirect:/login?emailAlterado";
            }

            atributos.addFlashAttribute("aviso", resultado.mudouAlgo()
                    ? "Dados atualizados." : "Nada a salvar: os dados já estavam assim.");
        } catch (final NegocioException excecao) {
            atributos.addFlashAttribute("erro", mensagemDe(excecao.getErro()));
            // O que a pessoa digitou volta para o formulario; so a senha, nunca.
            atributos.addFlashAttribute("nomeDigitado", nome);
            atributos.addFlashAttribute("emailDigitado", email);
        }

        return VOLTAR_AO_PERFIL;
    }

    /**
     * Entrega a foto da propria sessao. Sem parametro de identificador, de proposito: a foto de
     * uma conta so sai para quem esta autenticado nela.
     */
    @GetMapping("/conta/foto")
    public ResponseEntity<byte[]> foto(final Principal autenticado) {
        return gerenciarPerfil.buscarFoto(autenticado.getName())
                .map(foto -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(foto.tipoConteudo()))
                        .body(foto.conteudo()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/conta/foto")
    public String trocarFoto(@RequestParam(name = "foto", required = false) final MultipartFile arquivo,
                             final Principal autenticado,
                             final RedirectAttributes atributos) {
        if (arquivo == null || arquivo.isEmpty()) {
            atributos.addFlashAttribute("erro", "Escolha uma imagem para a foto.");

            return VOLTAR_AO_PERFIL;
        }

        try {
            gerenciarPerfil.trocarFoto(autenticado.getName(), arquivo.getBytes());
            atributos.addFlashAttribute("aviso", "Foto atualizada.");
        } catch (final NegocioException excecao) {
            atributos.addFlashAttribute("erro", mensagemDe(excecao.getErro()));
        } catch (final IOException excecao) {
            log.warn("[m=trocarFoto] Falha ao ler o arquivo enviado: {}", excecao.getMessage());
            atributos.addFlashAttribute("erro", mensagemDe(UsuarioErrorsMessage.FOTO_INVALIDA));
        }

        return VOLTAR_AO_PERFIL;
    }

    @PostMapping("/conta/foto/remover")
    public String removerFoto(final Principal autenticado, final RedirectAttributes atributos) {
        gerenciarPerfil.removerFoto(autenticado.getName());
        atributos.addFlashAttribute("aviso", "Foto removida. Aparece de novo a inicial do seu nome.");

        return VOLTAR_AO_PERFIL;
    }

    private String mensagemDe(final ErrorsMessage erro) {
        return messageSource.getMessage(erro.getChaveMensagem(), null, erro.getChaveMensagem(), PT_BR);
    }
}
