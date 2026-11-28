package br.edu.bibliotech.controller;

import br.edu.bibliotech.service.LivroService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final LivroService livros;

    public HomeController(LivroService livros) {
        this.livros = livros;
    }

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        model.addAttribute("livros", livros.listar());
        return "home";
    }
}
