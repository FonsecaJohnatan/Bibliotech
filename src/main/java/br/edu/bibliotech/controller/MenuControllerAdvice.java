package br.edu.bibliotech.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackageClasses = HomeController.class)
public class MenuControllerAdvice {

    @ModelAttribute("autenticado")
    public boolean autenticado(Authentication autenticacao) {
        return autenticacao != null && autenticacao.isAuthenticated()
                && !"anonymousUser".equals(autenticacao.getPrincipal());
    }

    @ModelAttribute("podeGerenciar")
    public boolean podeGerenciar(Authentication autenticacao) {
        if (!autenticado(autenticacao)) {
            return false;
        }

        return autenticacao.getAuthorities().stream()
                .anyMatch(permissao -> permissao.getAuthority().equals("ROLE_ADMIN")
                        || permissao.getAuthority().equals("ROLE_BIBLIOTECARIO"));
    }
}
