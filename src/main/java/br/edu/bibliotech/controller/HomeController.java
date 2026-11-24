package br.edu.bibliotech.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import br.edu.bibliotech.service.AutorService;
import br.edu.bibliotech.service.CategoriaService;

@Controller
public class HomeController {

	@Autowired
	private AutorService autorService;

	@Autowired
	private CategoriaService categoriaService;

	@GetMapping({"/", "/home"})
	public String home(Model model) {
		model.addAttribute("autores", this.autorService.listar());
		model.addAttribute("categorias", this.categoriaService.listar());
		return "home";
	}
}
