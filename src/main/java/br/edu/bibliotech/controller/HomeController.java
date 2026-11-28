package br.edu.bibliotech.controller;

import br.edu.bibliotech.dto.UsuarioLogado;
import br.edu.bibliotech.service.LivroService;
import br.edu.bibliotech.service.CategoriaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;

@Controller
public class HomeController {

    private final LivroService livros;
    private final CategoriaService categorias;

    public HomeController(LivroService livros, CategoriaService categorias) {
        this.livros = livros;
        this.categorias = categorias;
    }

    @GetMapping({"/", "/home"})
    public String home(@RequestParam(defaultValue = "") String busca,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(defaultValue = "false") boolean disponiveis,
            @SessionAttribute(name = "usuarioLogado", required = false) UsuarioLogado usuario, Model model) {
        if (usuario == null) {
            return "redirect:/login";
        }

        model.addAttribute("livros", livros.pesquisar(busca, categoriaId, disponiveis));
        model.addAttribute("categorias", categorias.listar());
        model.addAttribute("busca", busca);
        model.addAttribute("categoriaId", categoriaId);
        model.addAttribute("disponiveis", disponiveis);
        return "home";
    }
}
