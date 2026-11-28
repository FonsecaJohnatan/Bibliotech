const alternarMenu = document.getElementById("alternar-menu");
if (alternarMenu) {
    const menu = document.getElementById("menu-lateral");
    const fundoMenu = document.querySelector(".fundo-menu");
    const telaPequena = window.matchMedia("(max-width: 800px)");
    let preferenciaRecolhido = false;

    try {
        preferenciaRecolhido = localStorage.getItem("bibliotech.menuRecolhido") === "true";
    } catch {}

    function definirMenu(aberto) {
        document.body.classList.toggle("menu-recolhido", !aberto);
        alternarMenu.setAttribute("aria-expanded", String(aberto));
        alternarMenu.setAttribute("aria-label", aberto ? "Recolher menu" : "Expandir menu");
        alternarMenu.title = aberto ? "Recolher menu" : "Expandir menu";
        document.querySelector("main").inert = aberto && telaPequena.matches;
    }

    alternarMenu.addEventListener("click", () => {
        const abrir = document.body.classList.contains("menu-recolhido");
        definirMenu(abrir);
        if (!telaPequena.matches) {
            preferenciaRecolhido = !abrir;
            try {
                localStorage.setItem("bibliotech.menuRecolhido", String(!abrir));
            } catch {}
        }
    });

    function fecharMenu() {
        definirMenu(false);
        alternarMenu.focus();
    }

    fundoMenu.addEventListener("click", fecharMenu);
    document.addEventListener("keydown", event => {
        if (event.key === "Escape" && !document.body.classList.contains("menu-recolhido")) {
            fecharMenu();
        }
    });
    telaPequena.addEventListener("change", () => definirMenu(!telaPequena.matches && !preferenciaRecolhido));
    definirMenu(!telaPequena.matches && !preferenciaRecolhido);

    menu.querySelectorAll("a").forEach(link => {
        const destino = new URL(link.href).pathname;
        const paginaAtual = window.location.pathname;
        const pertenceAoMenu = paginaAtual === destino || paginaAtual.startsWith(destino + "/");
        if (pertenceAoMenu || (paginaAtual === "/" && destino.endsWith("/home"))) {
            link.setAttribute("aria-current", "page");
        }
    });
}

document.querySelectorAll("[data-dialogo]").forEach(link => {
    link.addEventListener("click", event => {
        const dialogo = document.getElementById(link.dataset.dialogo);
        if (dialogo && typeof dialogo.showModal === "function") {
            event.preventDefault();
            dialogo.showModal();
        }
    });
});

document.querySelectorAll("[data-fechar-dialogo]").forEach(link => {
    link.addEventListener("click", event => {
        const dialogo = link.closest("dialog");
        if (dialogo) {
            event.preventDefault();
            dialogo.close();
        }
    });
});

document.querySelectorAll("[data-filtrar-select]").forEach(campo => {
    const select = document.getElementById(campo.dataset.filtrarSelect);
    const opcoes = Array.from(select.options);
    campo.addEventListener("input", () => {
        const termo = campo.value.trim().toLocaleLowerCase("pt-BR");
        const selecionado = select.value;
        const filtradas = opcoes.filter(opcao =>
            !opcao.value || opcao.text.toLocaleLowerCase("pt-BR").includes(termo));
        select.replaceChildren(...filtradas);
        select.value = filtradas.some(opcao => opcao.value === selecionado) ? selecionado : "";
        select.dispatchEvent(new Event("change"));
    });
});

const formulario = document.getElementById("novo-emprestimo");
if (formulario) {
    const usuario = document.getElementById("usuarioId");
    const exemplar = document.getElementById("exemplarId");
    const confirmar = document.getElementById("confirmar-emprestimo");
    const aviso = document.getElementById("verificacao-aviso");
    const condicoes = Array.from(formulario.querySelectorAll("[data-condicao]"));
    let permitido = false;
    let sequencia = 0;

    function atualizarConfirmacao() {
        confirmar.disabled = !permitido || !exemplar.value;
    }

    async function verificarUsuario() {
        const requisicao = ++sequencia;
        permitido = false;
        atualizarConfirmacao();
        condicoes.forEach(item => item.classList.remove("condicao-ok", "condicao-erro"));
        aviso.textContent = usuario.value ? "Verificando usuário..." : "Selecione um usuário para verificar as condições.";
        if (!usuario.value) return;

        try {
            const resposta = await fetch(formulario.dataset.verificacaoUrl + "?usuarioId=" + encodeURIComponent(usuario.value),
                { headers: { Accept: "application/json" } });
            if (!resposta.ok) throw new Error("Falha na verificação");
            const dados = await resposta.json();
            if (requisicao !== sequencia) return;
            permitido = condicoes.every(item => dados[item.dataset.condicao] === true);
            condicoes.forEach(item => item.classList.add(dados[item.dataset.condicao] === true ? "condicao-ok" : "condicao-erro"));
            aviso.textContent = permitido ? "Usuário apto. Selecione um exemplar disponível." : "Empréstimo bloqueado. Confira as condições em vermelho.";
        } catch {
            if (requisicao !== sequencia) return;
            aviso.textContent = "Não foi possível verificar. Use Verificar seleção para tentar novamente.";
        }
        atualizarConfirmacao();
    }

    usuario.addEventListener("change", verificarUsuario);
    exemplar.addEventListener("change", atualizarConfirmacao);
    verificarUsuario();
}
