// Comportamento das telas. Fica em arquivo proprio porque a politica de conteudo do servidor
// nao aceita script embutido na pagina nem atributo onclick.

// Selecao da listagem de doadores: "selecionar todos", contador e botao que depende da selecao.
function atualizarSelecao(formulario) {
    const marcados = formulario.querySelectorAll('input[name="selecionados"]:checked').length;
    const contador = formulario.querySelector('[data-contador-selecao]');

    if (contador) {
        contador.hidden = marcados === 0;
        contador.textContent = marcados === 1 ? '1 selecionado' : marcados + ' selecionados';
    }
    formulario.querySelectorAll('[data-precisa-selecao]').forEach((botao) => { botao.disabled = marcados === 0; });
}

document.addEventListener('change', (evento) => {
    const origem = evento.target;
    const formulario = origem.closest('form');

    if (origem.matches('[data-marcar-todos]')) {
        formulario.querySelectorAll('input[name="selecionados"]:not([disabled])')
            .forEach((caixa) => { caixa.checked = origem.checked; });
    }

    if (formulario && formulario.matches('[data-selecao]')) {
        atualizarSelecao(formulario);
    }

    // Filtro que se aplica sozinho ao escolher um chip; o texto da busca espera o botao.
    if (formulario && formulario.matches('[data-filtro-automatico]') && origem.type !== 'search') {
        formulario.requestSubmit();
    }
});

// Botao com data-confirmar so segue adiante se a pessoa confirmar.
document.addEventListener('click', (evento) => {
    const botao = evento.target.closest('[data-confirmar]');

    if (botao && !window.confirm(botao.dataset.confirmar)) {
        evento.preventDefault();
    }
});

// Dialogos: data-abrir-dialogo="id" abre, data-fechar-dialogo fecha o dialogo em que esta.
document.addEventListener('click', (evento) => {
    const abrir = evento.target.closest('[data-abrir-dialogo]');
    const fechar = evento.target.closest('[data-fechar-dialogo]');

    if (abrir) {
        document.getElementById(abrir.dataset.abrirDialogo)?.showModal();
    }
    if (fechar) {
        fechar.closest('dialog')?.close();
    }
});

// Menus de tres pontos: abrir um fecha os outros, e clicar fora fecha todos.
document.addEventListener('click', (evento) => {
    document.querySelectorAll('details.menu-acoes[open], details.menu-config[open]').forEach((menu) => {
        if (!menu.contains(evento.target)) {
            menu.open = false;
        }
    });
});

document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('form[data-selecao]').forEach(atualizarSelecao);
});
