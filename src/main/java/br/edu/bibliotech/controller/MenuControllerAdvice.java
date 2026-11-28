package br.edu.bibliotech.controller;

import br.edu.bibliotech.dto.UsuarioLogado;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.SessionAttribute;

@ControllerAdvice(basePackageClasses = HomeController.class)
public class MenuControllerAdvice {

    @ModelAttribute("administrador")
    public boolean administrador(@SessionAttribute(name = "usuarioLogado", required = false) UsuarioLogado usuario) {
        return usuario != null && usuario.administrador();
    }

    @ModelAttribute("nomeUsuario")
    public String nomeUsuario(@SessionAttribute(name = "usuarioLogado", required = false) UsuarioLogado usuario) {
        return usuario == null ? "" : usuario.nome();
    }

    @ModelAttribute("autenticado")
    public boolean autenticado(@SessionAttribute(name = "usuarioLogado", required = false) UsuarioLogado usuario) {
        return usuario != null;
    }

    @ModelAttribute("podeGerenciar")
    public boolean podeGerenciar(@SessionAttribute(name = "usuarioLogado", required = false) UsuarioLogado usuario) {
        return usuario != null && usuario.podeGerenciar();
    }
}
