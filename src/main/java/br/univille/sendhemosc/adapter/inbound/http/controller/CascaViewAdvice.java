package br.univille.sendhemosc.adapter.inbound.http.controller;

import br.univille.sendhemosc.domain.enums.PerfilUsuario;
import br.univille.sendhemosc.domain.enums.SituacaoUsuario;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Alimenta a casca de navegacao com o que ela precisa em qualquer tela.
 *
 * <p>Hoje e so o numero de cadastros a espera de decisao, que aparece ao lado de Contas.
 * Sem ele o administrador so descobre que ha alguem esperando se abrir a tela por acaso,
 * e quem se cadastrou fica parado sem saber por que.</p>
 */
@ControllerAdvice
@RequiredArgsConstructor
public class CascaViewAdvice {

    private static final String AUTORIDADE_MASTER = "ROLE_" + PerfilUsuario.MASTER.name();

    private final IUsuarioRepositoryPort usuarioRepository;

    /**
     * Conta os cadastros pendentes, e somente para quem pode decidir sobre eles: os demais
     * perfis nao veem o indicador, entao nao vale consultar o banco a cada tela que abrem.
     *
     * @param autenticado usuario da requisicao, nulo nas telas publicas
     * @return total aguardando decisao, ou zero quando nao ha o que mostrar
     */
    @ModelAttribute("aprovacoesPendentes")
    public long aprovacoesPendentes(final Authentication autenticado) {
        if (!podeDecidir(autenticado)) {
            return 0L;
        }

        return usuarioRepository.listarPorSituacao(SituacaoUsuario.PENDENTE).size();
    }

    private boolean podeDecidir(final Authentication autenticado) {
        return autenticado != null
                && autenticado.isAuthenticated()
                && autenticado.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(AUTORIDADE_MASTER::equals);
    }
}
