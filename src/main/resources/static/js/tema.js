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

    // A engrenagem fecha ao clicar fora dela.
    document.addEventListener('click', function (evento) {
        document.querySelectorAll('details.menu-tema[open]').forEach(function (menu) {
            if (!menu.contains(evento.target)) {
                menu.open = false;
            }
        });
    });
})();
