// Tema escolhido na engrenagem da lateral: automatico (segue o sistema), claro ou escuro.
// Carregado no <head>, sem defer, para aplicar a escolha antes da primeira pintura: assim a
// pagina nao pisca no tema errado. A escolha fica so neste navegador.
(function () {
    var CHAVE = 'sendhemosc.tema';
    var raiz = document.documentElement;

    function lida() {
        try {
            var valor = window.localStorage.getItem(CHAVE);
            return valor === 'claro' || valor === 'escuro' ? valor : 'automatico';
        } catch (e) {
            return 'automatico';
        }
    }

    function aplicar(tema) {
        if (tema === 'automatico') {
            raiz.removeAttribute('data-tema');
        } else {
            raiz.setAttribute('data-tema', tema);
        }
    }

    aplicar(lida());

    document.addEventListener('change', function (evento) {
        var campo = evento.target;
        if (!campo.matches || !campo.matches('input[name="tema"]')) {
            return;
        }
        aplicar(campo.value);
        try {
            if (campo.value === 'automatico') {
                window.localStorage.removeItem(CHAVE);
            } else {
                window.localStorage.setItem(CHAVE, campo.value);
            }
        } catch (e) {
            // Sem armazenamento (janela anonima, bloqueio): vale so ate recarregar.
        }
    });

    document.addEventListener('DOMContentLoaded', function () {
        var atual = lida();
        document.querySelectorAll('input[name="tema"]').forEach(function (campo) {
            campo.checked = campo.value === atual;
        });
    });

    // Menu da engrenagem: o clique abre e fecha (no toque nao ha hover); clicar fora ou Esc fecha.
    // Fica aqui, e nao em sendhemosc.js, porque este arquivo esta em toda tela com a lateral.
    function fecharMenus(excecao) {
        document.querySelectorAll('.menu-lateral.aberto').forEach(function (menu) {
            if (menu !== excecao) {
                menu.classList.remove('aberto');
                menu.querySelector('[data-alternar-menu]').setAttribute('aria-expanded', 'false');
            }
        });
    }

    document.addEventListener('click', function (evento) {
        var botao = evento.target.closest && evento.target.closest('[data-alternar-menu]');
        var menu = botao ? botao.closest('.menu-lateral') : null;

        if (menu) {
            var abrir = !menu.classList.contains('aberto');
            menu.classList.toggle('aberto', abrir);
            botao.setAttribute('aria-expanded', String(abrir));
            if (!abrir) {
                botao.blur();
            }
        }
        fecharMenus(menu || (evento.target.closest && evento.target.closest('.menu-lateral')));
    });

    document.addEventListener('keydown', function (evento) {
        if (evento.key === 'Escape') {
            fecharMenus(null);
            if (document.activeElement && document.activeElement.closest('.menu-lateral')) {
                document.activeElement.blur();
            }
        }
    });
})();
