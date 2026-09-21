package br.univille.sendhemosc.adapter.outbound.email;

import br.univille.sendhemosc.domain.dto.MensagemEmail;
import br.univille.sendhemosc.domain.port.outbound.IEmailPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Alternativa que nao envia nada: apenas registra a mensagem no log.
 *
 * <p>Sempre disponivel, porque e para onde EnvioDeEmailRouter desvia quando nao ha provedor
 * configurado ou quando o responsavel desliga o envio pela tela.</p>
 */
@Slf4j
@Component
public class LogEmailAdapter implements IEmailPort {

    @Override
    public void enviar(final MensagemEmail mensagem) {
        log.info("[m=enviar] [MODO LOG - NADA FOI ENVIADO] para={} assunto={} tamanhoCorpo={}",
                mensagem.destinatario(), mensagem.assunto(), mensagem.corpoHtml().length());
    }
}
