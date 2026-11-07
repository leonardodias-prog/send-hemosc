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

// Editar conta: o dialogo e um so, preenchido com os dados da linha escolhida.
document.addEventListener('click', (evento) => {
    const botao = evento.target.closest('[data-editar-conta]');
    const dialogo = document.getElementById('editar-conta');

    if (!botao || !dialogo) {
        return;
    }

    const formulario = dialogo.querySelector('form[data-form-edicao]');
    formulario.action = formulario.action.replace(/\/usuarios\/[^/]+$/, '/usuarios/' + botao.dataset.id);
    formulario.elements.nome.value = botao.dataset.nome;
    formulario.elements.email.value = botao.dataset.email;
    formulario.elements.perfil.value = botao.dataset.perfil;
    formulario.elements.situacao.value = botao.dataset.situacao;
    botao.closest('details')?.removeAttribute('open');
    dialogo.showModal();
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

// Perfil: a foto escolhida segue sozinha para o servidor, depois de conferir o tamanho aqui (o
// servidor confere de novo; isto so poupa a pessoa de esperar um envio que seria recusado).
document.addEventListener('change', (evento) => {
    const campo = evento.target;

    if (!campo.matches('[data-foto-input]') || !campo.files.length) {
        return;
    }

    const erro = document.querySelector('[data-foto-erro]');
    const grande = campo.files[0].size > Number(campo.dataset.limiteBytes);

    if (erro) {
        erro.hidden = !grande;
    }
    if (grande) {
        campo.value = '';
        return;
    }
    campo.form.requestSubmit();
});

// Perfil: a senha atual so e pedida quando o e-mail muda. Sem JavaScript o campo fica sempre a
// vista, e o servidor recusa a troca sem a senha de qualquer jeito.
function conferirTrocaDeEmail(formulario) {
    const original = formulario.dataset.emailOriginal.trim().toLowerCase();
    const mudou = formulario.elements.email.value.trim().toLowerCase() !== original;

    formulario.querySelector('[data-confirma-email]').hidden = !mudou;
    formulario.elements.senhaAtual.required = mudou;
}

document.addEventListener('input', (evento) => {
    const formulario = evento.target.closest('form[data-dados-form]');

    if (formulario && evento.target.name === 'email') {
        conferirTrocaDeEmail(formulario);
    }
});

document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('form[data-dados-form]').forEach(conferirTrocaDeEmail);
});
