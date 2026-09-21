package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.domain.dto.UsuarioResumo;
import br.univille.sendhemosc.domain.enums.SituacaoUsuario;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import br.univille.sendhemosc.usecase.usuario.ResolverAprovacaoUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Gerenciamento de contas, restrito ao administrador, e os links de decisao enviados por
 * e-mail. Os links ficam publicos porque quem os recebe ainda nao esta autenticado ao clicar;
 * o que os protege e o token, aleatorio e de uso unico.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class UsuarioViewAdapter {

    private final IUsuarioRepositoryPort usuarioRepository;
    private final ResolverAprovacaoUseCase resolverAprovacao;

    @GetMapping("/usuarios")
    public String listar(final Model model) {
        final List<UsuarioResumo> todos = usuarioRepository.listarTodos();

        model.addAttribute("usuarios", todos);
        model.addAttribute("pendentes", todos.stream()
                .filter(usuario -> usuario.situacao() == SituacaoUsuario.PENDENTE)
                .count());

        return "usuarios";
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
        try {
            final UsuarioResumo usuario = resolverAprovacao.porTela(id, aprovado, null);
            atributos.addFlashAttribute("aviso", "%s de %s %s."
                    .formatted(usuario.perfil().getDescricao(), usuario.nome(),
                            aprovado ? "aprovado" : "recusado"));
        } catch (final NegocioException excecao) {
            atributos.addFlashAttribute("erro", "Este cadastro já foi decidido por alguém.");
        }

        return "redirect:/usuarios";
    }

    @GetMapping(value = "/aprovacao/{token}/{decisao}", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> decidirPorLink(@PathVariable final String token,
                                                 @PathVariable final String decisao) {
        final boolean aprovado = "aprovar".equals(decisao);

        if (!aprovado && !"recusar".equals(decisao)) {
            return ResponseEntity.badRequest().body(pagina("Link inválido",
                    "O endereço acessado não corresponde a uma decisão válida."));
        }

        try {
            final UsuarioResumo usuario = resolverAprovacao.porToken(token, aprovado);

            return ResponseEntity.ok(pagina(
                    aprovado ? "Cadastro aprovado" : "Cadastro recusado",
                    "%s (%s) foi %s como %s.".formatted(usuario.nome(), usuario.email(),
                            aprovado ? "liberado" : "recusado", usuario.perfil().getDescricao())));
        } catch (final NegocioException excecao) {
            return ResponseEntity.status(excecao.getErro().getStatus()).body(pagina("Link já utilizado",
                    "Este cadastro já foi decidido. Cada link de aprovação vale uma vez só."));
        }
    }

    private String pagina(final String titulo, final String mensagem) {
        return """
                <!DOCTYPE html><html lang="pt-BR"><head><meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Send Hemosc</title>
                <style>body{font-family:system-ui,Arial,sans-serif;max-width:32rem;margin:4rem auto;
                padding:0 1rem;line-height:1.6;color:#18181b}h1{font-size:1.3rem}
                a{color:#b91c1c}</style></head><body>
                <h1>%s</h1><p>%s</p><p><a href="/usuarios">Ir para o gerenciamento de contas</a></p>
                </body></html>""".formatted(titulo, mensagem);
    }
}
