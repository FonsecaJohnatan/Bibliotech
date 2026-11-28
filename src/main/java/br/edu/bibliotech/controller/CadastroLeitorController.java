package br.edu.bibliotech.controller;

import br.edu.bibliotech.dto.RequisicaoNovoLeitor;
import br.edu.bibliotech.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class CadastroLeitorController {

    private final UsuarioService usuarioService;

    public CadastroLeitorController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/cadastro")
    public String formulario(RequisicaoNovoLeitor requisicao) {
        return "usuario/cadastro";
    }

    @PostMapping("/cadastro")
    public String cadastrar(@Valid RequisicaoNovoLeitor requisicao, BindingResult result) {
        if (result.hasErrors()) {
            return "usuario/cadastro";
        }

        try {
            usuarioService.cadastrar(requisicao.toEntidade());
        } catch (IllegalArgumentException erro) {
            result.reject("cadastro", erro.getMessage());
            return "usuario/cadastro";
        }

        return "redirect:/login?cadastro";
    }
}
