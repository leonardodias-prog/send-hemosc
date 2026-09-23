package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.domain.dto.UsuarioResumo;
import br.univille.sendhemosc.domain.enums.PerfilUsuario;
import br.univille.sendhemosc.domain.enums.SituacaoUsuario;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import br.univille.sendhemosc.usecase.usuario.GerenciarUsuarioUseCase;
import br.univille.sendhemosc.usecase.usuario.ResolverAprovacaoUseCase;
import java.security.Principal;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Gerenciamento de contas, restrito ao administrador, e os links de decisao enviados por
 * e-mail. Os links ficam publicos porque quem os recebe ainda nao esta autenticado ao clicar;
 * o que os protege e o token, aleatorio e de uso unico.
 *
 * <p>Toda tela daqui devolve o proximo passo em vez de terminar em si mesma.</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class UsuarioViewAdapter {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final IUsuarioRepositoryPort usuarioRepository;
    private final ResolverAprovacaoUseCase resolverAprovacao;
    private final GerenciarUsuarioUseCase gerenciarUsuario;
    private final MessageSource messageSource;

    @GetMapping("/usuarios")
    public String listar(final Model model) {
        final List<UsuarioResumo> todos = usuarioRepository.listarTodos();

        model.addAttribute("usuarios", todos);
        model.addAttribute("pendentes", todos.stream()
                .filter(usuario -> usuario.situacao() == SituacaoUsuario.PENDENTE)
                .count());
        model.addAttribute("perfis", PerfilUsuario.values());
        model.addAttribute("situacoes", SituacaoUsuario.values());

        return "usuarios";
    }

    @PostMapping("/usuarios")
    public String criar(@RequestParam final String nome,
                        @RequestParam final String email,
                        @RequestParam final String senha,
                        @RequestParam final PerfilUsuario perfil,
                        final RedirectAttributes atributos) {
        return executar(atributos, () -> {
            final UsuarioResumo criado = gerenciarUsuario.criar(nome, email, senha, perfil);
            return "Conta de %s criada como %s.".formatted(criado.nome(), perfil.getDescricao());
        });
    }

    @PostMapping("/usuarios/{id}")
    public String atualizar(@PathVariable final Long id,
                            @RequestParam final String nome,
                            @RequestParam final String email,
                            @RequestParam final PerfilUsuario perfil,
                            @RequestParam final SituacaoUsuario situacao,
                            final Principal autenticado,
                            final RedirectAttributes atributos) {
        return executar(atributos, () -> {
            final UsuarioResumo alterado = gerenciarUsuario
                    .atualizar(id, nome, email, perfil, situacao, autenticado.getName());
            return "Conta de %s atualizada.".formatted(alterado.nome());
        });
    }

    @PostMapping("/usuarios/{id}/senha")
    public String redefinirSenha(@PathVariable final Long id,
                                 @RequestParam(required = false) final String senha,
                                 final RedirectAttributes atributos) {
        return executar(atributos, () -> {
            final var resultado = gerenciarUsuario.redefinirSenha(id, senha);

            if (resultado.senhaGerada() == null) {
                return "Senha de %s redefinida.".formatted(resultado.usuario().nome());
            }

            // A senha gerada aparece uma unica vez: nao fica guardada em lugar nenhum.
            return "Senha de %s redefinida para: %s — anote agora, ela não será exibida de novo."
                    .formatted(resultado.usuario().nome(), resultado.senhaGerada());
        });
    }

    @PostMapping("/usuarios/{id}/excluir")
    public String excluir(@PathVariable final Long id,
                          final Principal autenticado,
                          final RedirectAttributes atributos) {
        return executar(atributos, () -> {
            final UsuarioResumo removido = gerenciarUsuario.excluir(id, autenticado.getName());
            return "Conta de %s excluída.".formatted(removido.nome());
        });
    }

    @PostMapping("/usuarios/{id}/aprovar")
    public String aprovar(@PathVariable final Long id, final RedirectAttributes atributos) {
        return decidir(id, true, atributos);
    }

    @PostMapping("/usuarios/{id}/recusar")
    public String recusar(@PathVariable final Long id, final RedirectAttributes atributos) {
        return decidir(id, false, atributos);
    }

    private String decidir(final Long id, final boolean aprovado, final RedirectAttributes atributos) {
        return executar(atributos, () -> {
            final UsuarioResumo usuario = resolverAprovacao.porTela(id, aprovado, null);
            return "%s de %s %s.".formatted(usuario.perfil().getDescricao(), usuario.nome(),
                    aprovado ? "aprovado" : "recusado");
        });
    }

    /**
     * Executa a acao traduzindo erro de negocio em mensagem para a tela, de modo que cada
     * metodo cuide apenas do que lhe cabe.
     *
     * @param atributos destino das mensagens
     * @param acao operacao a executar, devolvendo o texto de sucesso
     * @return redirecionamento para a listagem
     */
    private String executar(final RedirectAttributes atributos, final Acao acao) {
        try {
            atributos.addFlashAttribute("aviso", acao.executar());
        } catch (final NegocioException excecao) {
            atributos.addFlashAttribute("erro", messageSource.getMessage(
                    excecao.getErro().getChaveMensagem(), null,
                    excecao.getErro().getChaveMensagem(), PT_BR));
        }

        return "redirect:/usuarios";
    }

    @GetMapping("/aprovacao/{token}/aprovar")
    public String aprovarPorLink(@PathVariable final String token, final Model model) {
        return decidirPorLink(token, true, model);
    }

    @GetMapping("/aprovacao/{token}/recusar")
    public String recusarPorLink(@PathVariable final String token, final Model model) {
        return decidirPorLink(token, false, model);
    }

    /**
     * Desfecho do link de decisao recebido por e-mail. Um endereco por decisao: o que nao
     * corresponde a nenhum dos dois nao chega ate aqui, entao nao sobra validacao a fazer.
     *
     * @param token token de uso unico recebido no e-mail
     * @param aprovado true para liberar a conta, false para recusar
     * @param model destino do texto exibido
     * @return pagina de desfecho
     */
    private String decidirPorLink(final String token, final boolean aprovado, final Model model) {
        try {
            final UsuarioResumo usuario = resolverAprovacao.porToken(token, aprovado);

            model.addAttribute("sucesso", true);
            model.addAttribute("titulo", aprovado ? "Cadastro aprovado" : "Cadastro recusado");
            model.addAttribute("mensagem", "%s (%s) foi %s como %s.".formatted(usuario.nome(),
                    usuario.email(), aprovado ? "liberado" : "recusado", usuario.perfil().getDescricao()));
        } catch (final NegocioException excecao) {
            log.debug("Link de aprovacao ja utilizado ou invalido: {}", excecao.getErro().getCodigo());

            model.addAttribute("sucesso", false);
            model.addAttribute("titulo", "Link já utilizado");
            model.addAttribute("mensagem",
                    "Este cadastro já foi decidido. Cada link de aprovação vale uma vez só.");
        }

        return "aprovacao";
    }

    /**
     * Operacao de gerenciamento que devolve a mensagem de sucesso.
     */
    @FunctionalInterface
    private interface Acao {

        String executar();
    }
}
