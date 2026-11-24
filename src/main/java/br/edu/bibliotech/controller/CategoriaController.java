package br.edu.bibliotech.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import br.edu.bibliotech.dto.RequisicaoNovaCategoria;
import br.edu.bibliotech.model.Categoria;
import br.edu.bibliotech.service.CategoriaService;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/categoria")
public class CategoriaController {

	@Autowired
	private CategoriaService categoriaService;

	@GetMapping
	public String listar(Model model) {
		model.addAttribute("categorias", this.categoriaService.listar());
		return "categoria/lista";
	}

	@GetMapping("formulario")
	public String formulario(RequisicaoNovaCategoria requisicao, Model model) {
		return "categoria/formulario";
	}

	@PostMapping("novo")
	public String novo(@Valid RequisicaoNovaCategoria requisicao, BindingResult result, Model model) {
		if(result.hasErrors()) {
			return "categoria/formulario";
		}

		try {
			Categoria categoria = requisicao.toCategoria();
			this.categoriaService.cadastrar(categoria);
		} catch (IllegalArgumentException exception) {
			result.reject("cadastro", exception.getMessage());
			return "categoria/formulario";
		}

		return "redirect:/home";
	}

	@GetMapping("/{id}/editar")
	public String editar(@PathVariable Long id, Model model) {
		Categoria categoria = categoriaService.buscarPorId(id);
		RequisicaoNovaCategoria requisicao = new RequisicaoNovaCategoria();
		requisicao.setNome(categoria.getNome());
		requisicao.setDescricao(categoria.getDescricao());

		model.addAttribute("requisicaoNovaCategoria", requisicao);
		model.addAttribute("id", id);
		return "categoria/formulario";
	}

	@PostMapping("/{id}")
	public String atualizar(@PathVariable Long id,
			@Valid RequisicaoNovaCategoria requisicao,
			BindingResult result, Model model) {
		model.addAttribute("id", id);

		if(result.hasErrors()) {
			return "categoria/formulario";
		}

		try {
			Categoria categoria = requisicao.toCategoria();
			this.categoriaService.atualizar(id, categoria);
		} catch (IllegalArgumentException exception) {
			result.reject("atualizacao", exception.getMessage());
			return "categoria/formulario";
		}

		return "redirect:/home";
	}
}
