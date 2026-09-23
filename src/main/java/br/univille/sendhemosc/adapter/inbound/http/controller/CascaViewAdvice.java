package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.domain.enums.PerfilUsuario;
import br.univille.sendhemosc.domain.enums.SituacaoUsuario;
import br.univille.sendhemosc.domain.port.outbound.IDisparoAutomaticoRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Alimenta a casca de navegacao com o que ela precisa em qualquer tela.
 *
 * <p>Sao dois indicadores ao lado dos itens da lateral. O numero de cadastros a espera de
 * decisao, ao lado de Contas: sem ele o administrador so descobre que ha alguem esperando se abrir
 * a tela por acaso. E o aviso de disparo automatico ligado: ligado, ele manda e-mail sem ninguem
 * clicar, e isso precisa estar a vista de quem pode desliga-lo.</p>
 *
 * <p>Cada indicador so e calculado para quem o enxerga: os demais perfis nao veem o item, entao
 * nao vale consultar o banco a cada tela que abrem.</p>
 */
@ControllerAdvice
@RequiredArgsConstructor
public class CascaViewAdvice {

    private static final String PREFIXO = "ROLE_";
    private static final Set<String> QUEM_DECIDE_CADASTRO = Set.of(PREFIXO + PerfilUsuario.MASTER.name());
    private static final Set<String> QUEM_CONFIGURA_DISPARO = Set.of(
            PREFIXO + PerfilUsuario.RESPONSAVEL.name(), PREFIXO + PerfilUsuario.MASTER.name());

    private final IUsuarioRepositoryPort usuarioRepository;
    private final IDisparoAutomaticoRepositoryPort disparoRepository;

    /**
     * Conta os cadastros pendentes, somente para quem pode decidir sobre eles.
     *
     * @param autenticado usuario da requisicao, nulo nas telas publicas
     * @return total aguardando decisao, ou zero quando nao ha o que mostrar
     */
    @ModelAttribute("aprovacoesPendentes")
    public long aprovacoesPendentes(final Authentication autenticado) {
        if (!temAlgumPerfil(autenticado, QUEM_DECIDE_CADASTRO)) {
            return 0L;
        }

        return usuarioRepository.listarPorSituacao(SituacaoUsuario.PENDENTE).size();
    }

    /**
     * Informa se o disparo automatico esta ligado, somente para quem pode configura-lo.
     *
     * @param autenticado usuario da requisicao, nulo nas telas publicas
     * @return true quando ligado e visivel para o perfil
     */
    @ModelAttribute("disparoAutomaticoLigado")
    public boolean disparoAutomaticoLigado(final Authentication autenticado) {
        return temAlgumPerfil(autenticado, QUEM_CONFIGURA_DISPARO) && disparoRepository.buscar().ativo();
    }

    private boolean temAlgumPerfil(final Authentication autenticado, final Set<String> autoridades) {
        return autenticado != null
                && autenticado.isAuthenticated()
                && autenticado.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(autoridades::contains);
    }
}
