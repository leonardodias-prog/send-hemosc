package br.univille.sendhemosc.adapter.outbound.email;

import br.univille.sendhemosc.domain.dto.MensagemEmail;
import br.univille.sendhemosc.domain.port.outbound.IEmailPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Decide, a cada mensagem, se ela sai de verdade ou apenas vai para o log.
 *
 * <p>A escolha do provedor continua sendo de configuracao e so muda com reinicio. O que este
 * roteador acrescenta e a decisao imediata: mesmo com a entrega configurada, o responsavel
 * pode interromper os disparos pela tela, sem depender de quem administra o ambiente.</p>
 *
 * <p>Sem provedor configurado, nada muda de comportamento: as mensagens sempre vao para o log,
 * e o interruptor nao tem efeito.</p>
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class EnvioDeEmailRouter implements IEmailPort {

    private final LogEmailAdapter registroEmLog;
    private final ObjectProvider<BrevoEmailAdapter> entregaReal;
    private final ControleDeEnvio controle;

    /**
     * Indica se existe provedor de entrega configurado neste ambiente.
     *
     * @return true quando ha um adapter capaz de entregar de verdade
     */
    public boolean temProvedorConfigurado() {
        return entregaReal.getIfAvailable() != null;
    }

    /**
     * Indica se as mensagens estao saindo neste momento.
     *
     * @return true somente quando ha provedor e o interruptor esta ligado
     */
    public boolean isEntregando() {
        return temProvedorConfigurado() && controle.isAtivo();
    }

    @Override
    public void enviar(final MensagemEmail mensagem) {
        if (isEntregando()) {
            entregaReal.getObject().enviar(mensagem);
            return;
        }

        registroEmLog.enviar(mensagem);
    }
}
