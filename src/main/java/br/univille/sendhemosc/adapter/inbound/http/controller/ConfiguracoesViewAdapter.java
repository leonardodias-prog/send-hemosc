package br.univille.sendhemosc.adapter.inbound.http.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Tela de configuracoes: reune o que nao e trabalho do dia a dia da captacao (aparencia, a
 * propria senha, o termo de uso e, para o administrador, contas e auditoria). Assim a lateral
 * fica so com as telas de uso diario.
 */
@Controller
public class ConfiguracoesViewAdapter {

    @GetMapping("/configuracoes")
    public String exibir() {
        return "configuracoes";
    }
}
