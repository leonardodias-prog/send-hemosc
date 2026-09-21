package br.univille.sendhemosc.adapter.outbound.email;

import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Interruptor de envio, acionavel em tempo de execucao pelo responsavel.
 *
 * <p>O modo de envio e escolhido na configuracao e so muda com reinicio. Isso nao atende quem
 * precisa interromper disparos agora, durante um problema ou uma apresentacao. O interruptor
 * existe para essa decisao imediata: desligado, as mensagens apenas vao para o log.</p>
 *
 * <p>O estado vive em memoria e volta ao configurado a cada reinicio. E o comportamento
 * desejado: desligar por causa de um incidente nao deve permanecer esquecido depois, e ligar
 * nunca deve sobreviver a um reinicio sem alguem decidir de novo.</p>
 */
@Slf4j
@Component
public class ControleDeEnvio {

    private final AtomicBoolean ativo;

    public ControleDeEnvio(@Value("${sendhemosc.email.ativo-na-subida:true}") final boolean ativoNaSubida) {
        this.ativo = new AtomicBoolean(ativoNaSubida);
    }

    public boolean isAtivo() {
        return ativo.get();
    }

    /**
     * Liga ou desliga o envio.
     *
     * @param novoEstado true para permitir a entrega real
     * @return true se o estado mudou de fato
     */
    public boolean definir(final boolean novoEstado) {
        final boolean anterior = ativo.getAndSet(novoEstado);

        if (anterior != novoEstado) {
            log.warn("[m=definir] Envio de e-mail {} em tempo de execucao",
                    novoEstado ? "LIGADO" : "DESLIGADO");
        }

        return anterior != novoEstado;
    }
}
