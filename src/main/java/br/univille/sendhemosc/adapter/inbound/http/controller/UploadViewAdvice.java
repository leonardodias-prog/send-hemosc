package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.domain.exception.UsuarioErrorsMessage;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Devolve arquivo grande demais como mensagem na tela de perfil.
 *
 * <p>O limite do servidor corta o envio antes de o controlador ser chamado, entao o caso de uso
 * nunca chega a ver o arquivo. Sem esta captura, a pessoa receberia a pagina de erro do
 * servidor em vez de um aviso dizendo o que fazer.</p>
 */
@ControllerAdvice
@RequiredArgsConstructor
public class UploadViewAdvice {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final MessageSource messageSource;

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String arquivoGrandeDemais(final RedirectAttributes atributos) {
        final String chave = UsuarioErrorsMessage.FOTO_GRANDE_DEMAIS.getChaveMensagem();
        atributos.addFlashAttribute("erro", messageSource.getMessage(chave, null, chave, PT_BR));

        return "redirect:/conta";
    }
}
