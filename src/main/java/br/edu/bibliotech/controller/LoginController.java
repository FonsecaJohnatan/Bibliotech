package br.edu.bibliotech.controller;

import br.edu.bibliotech.dto.UsuarioLogado;
import br.edu.bibliotech.model.Usuario;
import br.edu.bibliotech.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    private final UsuarioService usuarioService;

    public LoginController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/login")
    public String formulario() {
        return "login";
    }

    @PostMapping("/login")
    public String entrar(@RequestParam(defaultValue = "") String email,
            @RequestParam(defaultValue = "") String password, HttpServletRequest requisicao) {
        Usuario usuario = usuarioService.autenticar(email, password);

        if (usuario == null) {
            return "redirect:/login?error";
        }
        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            return "redirect:/login?inativo";
        }

        iniciarSessao(requisicao, usuario);
        return "redirect:/home";
    }

    @PostMapping("/sair")
    public String sair(HttpServletRequest requisicao) {
        HttpSession sessao = requisicao.getSession(false);
        if (sessao != null) {
            sessao.invalidate();
        }

        return "redirect:/login?logout";
    }

    private void iniciarSessao(HttpServletRequest requisicao, Usuario usuario) {
        HttpSession anterior = requisicao.getSession(false);
        if (anterior != null) {
            anterior.invalidate();
        }

        UsuarioLogado usuarioLogado = new UsuarioLogado(usuario.getId(), usuario.getNome(), usuario.getPerfil());
        requisicao.getSession(true).setAttribute("usuarioLogado", usuarioLogado);
    }
}
