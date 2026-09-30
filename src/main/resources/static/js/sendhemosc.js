// Comportamento das telas. Fica em arquivo proprio porque a politica de conteudo do servidor
// nao aceita script embutido na pagina nem atributo onclick.

// Caixa "selecionar todos" da listagem de doadores.
document.addEventListener('change', (evento) => {
    const origem = evento.target;

    if (!origem.matches('[data-marcar-todos]')) {
        return;
    }

    document.querySelectorAll('input[name="selecionados"]:not([disabled])')
        .forEach((caixa) => { caixa.checked = origem.checked; });
});

// Botao com data-confirmar so segue adiante se a pessoa confirmar.
document.addEventListener('click', (evento) => {
    const botao = evento.target.closest('[data-confirmar]');

    if (botao && !window.confirm(botao.dataset.confirmar)) {
        evento.preventDefault();
    }
});
