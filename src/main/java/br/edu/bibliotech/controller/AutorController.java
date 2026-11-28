package br.edu.bibliotech.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import br.edu.bibliotech.dto.RequisicaoNovoAutor;
import br.edu.bibliotech.model.Autor;
import br.edu.bibliotech.service.AutorService;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/autor")
public class AutorController {

	@Autowired
	private AutorService autorService;

	@GetMapping
	public String listar(Model model) {
		model.addAttribute("autores", this.autorService.listar());
		return "autor/lista";
	}

	@GetMapping("formulario")
	public String formulario(RequisicaoNovoAutor requisicao, Model model) {
		return "autor/formulario";
	}

	@PostMapping("novo")
	public String novo(@Valid RequisicaoNovoAutor requisicao, BindingResult result, Model model) {
		if(result.hasErrors()) {
			return "autor/formulario";
		}

		try {
			Autor autor = requisicao.toEntidade();
			this.autorService.cadastrar(autor);
		} catch (IllegalArgumentException exception) {
			result.reject("cadastro", exception.getMessage());
			return "autor/formulario";
		}

		return "redirect:/home";
	}

	@GetMapping("/{id}/editar")
	public String editar(@PathVariable Long id, Model model) {
		Autor autor = autorService.buscarPorId(id);
		RequisicaoNovoAutor requisicao = new RequisicaoNovoAutor();
		requisicao.setNome(autor.getNome());
		requisicao.setNacionalidade(autor.getNacionalidade());
		requisicao.setDataNascimento(autor.getDataNascimento());

		model.addAttribute("requisicaoNovoAutor", requisicao);
		model.addAttribute("id", id);
		return "autor/formulario";
	}

	@PostMapping("/{id}")
	public String atualizar(@PathVariable Long id,
			@Valid RequisicaoNovoAutor requisicao,
			BindingResult result, Model model) {
		model.addAttribute("id", id);

		if(result.hasErrors()) {
			return "autor/formulario";
		}

		try {
			Autor autor = requisicao.toEntidade();
			this.autorService.atualizar(id, autor);
		} catch (IllegalArgumentException exception) {
			result.reject("atualizacao", exception.getMessage());
			return "autor/formulario";
		}

		return "redirect:/home";
	}
}
