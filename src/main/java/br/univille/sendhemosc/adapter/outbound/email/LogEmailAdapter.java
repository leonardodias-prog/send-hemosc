package br.univille.sendhemosc.adapter.outbound.email;

import br.univille.sendhemosc.domain.dto.MensagemEmail;
import br.univille.sendhemosc.domain.port.outbound.IEmailPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Implementacao padrao do envio de e-mail: nao envia nada, apenas registra a mensagem no log.
 * E o comportamento seguro para um prototipo alimentado por dados ficticios, onde um disparo
 * acidental para um endereco real seria um incidente com a instituicao parceira.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sendhemosc.email.modo", havingValue = "log", matchIfMissing = true)
public class LogEmailAdapter implements IEmailPort {

    @Override
    public void enviar(final MensagemEmail mensagem) {
        log.info("[m=enviar] [MODO LOG - NADA FOI ENVIADO] para={} assunto={} tamanhoCorpo={}",
                mensagem.destinatario(), mensagem.assunto(), mensagem.corpoHtml().length());
    }
}
