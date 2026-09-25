package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.domain.dto.DadosDoTitular;
import br.univille.sendhemosc.domain.dto.DoadorCadastrado;
import br.univille.sendhemosc.domain.dto.NovoDoador;
import br.univille.sendhemosc.domain.enums.Sexo;
import br.univille.sendhemosc.domain.enums.TipoSanguineo;
import br.univille.sendhemosc.domain.exception.DoadorErrorsMessage;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.port.outbound.IDoadorRepositoryPort;
import br.univille.sendhemosc.usecase.doador.AlterarSituacaoDoadorUseCase;
import br.univille.sendhemosc.usecase.doador.EditarDoadorUseCase;
import br.univille.sendhemosc.usecase.doador.ExcluirDoadorUseCase;
import br.univille.sendhemosc.usecase.doador.ExportarDadosDoTitularUseCase;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Manutencao do cadastro de um doador: editar, desativar, reativar, excluir e exportar os dados.
 *
 * <p>Ate aqui o sistema so cadastrava. Um e-mail digitado errado ficava errado para sempre, e
 * quem deixou de doar continuava na base sendo convocado.</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class CadastroDoadorViewAdapter {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final IDoadorRepositoryPort doadorRepository;
    private final EditarDoadorUseCase editarDoador;
    private final AlterarSituacaoDoadorUseCase alterarSituacao;
    private final ExcluirDoadorUseCase excluirDoador;
    private final ExportarDadosDoTitularUseCase exportarDados;
    private final MessageSource messageSource;

    @GetMapping("/doadores/{id}/editar")
    public String formulario(@PathVariable final Long id, final Model model, final RedirectAttributes atributos) {
        final Optional<DoadorCadastrado> doador = doadorRepository.buscarPorId(id);

        if (doador.isEmpty()) {
            atributos.addFlashAttribute("erro", mensagem(DoadorErrorsMessage.NAO_ENCONTRADO.getChaveMensagem()));
            return "redirect:/doadores";
        }

        if (!model.containsAttribute("dadosDoador")) {
            model.addAttribute("dadosDoador", doador.get().comoFormulario());
        }

        return prepararTela(doador.get(), model);
    }

    @PostMapping("/doadores/{id}/editar")
    public String editar(@PathVariable final Long id,
                         @Valid @ModelAttribute("dadosDoador") final NovoDoador dados,
                         final BindingResult validacao,
                         final Model model,
                         final RedirectAttributes atributos) {
        final DoadorCadastrado doador = doadorRepository.buscarPorId(id)
                .orElseThrow(() -> new NegocioException(DoadorErrorsMessage.NAO_ENCONTRADO));

        if (!validacao.hasErrors()) {
            try {
                editarDoador.execute(id, dados);
                atributos.addFlashAttribute("aviso", "Cadastro de %s atualizado.".formatted(dados.nome().trim()));
                return "redirect:/doadores/" + id + "/editar";
            } catch (final NegocioException excecao) {
                validacao.rejectValue("email", excecao.getErro().getCodigo(),
                        mensagem(excecao.getErro().getChaveMensagem()));
            }
        }

        // Com erro, a tela e renderizada de novo com o que foi digitado, para nao obrigar a redigitar.
        return prepararTela(doador, model);
    }

    @PostMapping("/doadores/{id}/desativar")
    public String desativar(@PathVariable final Long id, final RedirectAttributes atributos) {
        final String nome = alterarSituacao.execute(id, false);
        atributos.addFlashAttribute("aviso", "%s foi desativado: não aparece mais na busca nem recebe convocações."
                .formatted(nome));

        return "redirect:/doadores/" + id + "/editar";
    }

    @PostMapping("/doadores/{id}/reativar")
    public String reativar(@PathVariable final Long id, final RedirectAttributes atributos) {
        final String nome = alterarSituacao.execute(id, true);
        atributos.addFlashAttribute("aviso", "%s foi reativado e volta a aparecer na busca.".formatted(nome));

        return "redirect:/doadores/" + id + "/editar";
    }

    /**
     * Exige a caixa de confirmacao marcada: a exclusao apaga tambem doacoes e convocacoes, e nao
     * tem volta. Sem ela, um clique no botao errado bastaria.
     */
    @PostMapping("/doadores/{id}/excluir")
    public String excluir(@PathVariable final Long id,
                          @RequestParam(defaultValue = "false") final boolean confirmacao,
                          final RedirectAttributes atributos) {
        if (!confirmacao) {
            atributos.addFlashAttribute("erro", "Marque a confirmação para excluir o doador.");
            return "redirect:/doadores/" + id + "/editar";
        }

        excluirDoador.porId(id);
        atributos.addFlashAttribute("aviso", "Doador excluído, junto com as doações, convocações e consentimentos dele.");

        return "redirect:/doadores";
    }

    @GetMapping("/doadores/{id}/exportar")
    @ResponseBody
    public ResponseEntity<DadosDoTitular> exportar(@PathVariable final Long id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("dados-doador-%d.json".formatted(id))
                        .build()
                        .toString())
                .body(exportarDados.porId(id));
    }

    private String prepararTela(final DoadorCadastrado doador, final Model model) {
        model.addAttribute("doador", doador);
        model.addAttribute("tiposSanguineos", TipoSanguineo.values());
        model.addAttribute("sexos", Sexo.values());

        return "doador-edicao";
    }

    private String mensagem(final String chave) {
        return messageSource.getMessage(chave, null, chave, PT_BR);
    }
}
