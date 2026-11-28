package br.edu.bibliotech.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import br.edu.bibliotech.dto.RequisicaoNovoLivro;
import br.edu.bibliotech.model.Autor;
import br.edu.bibliotech.model.Livro;
import br.edu.bibliotech.service.AutorService;
import br.edu.bibliotech.service.CategoriaService;
import br.edu.bibliotech.service.LivroService;
import br.edu.bibliotech.service.ExemplarService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/livro")
public class LivroController {

	@Autowired
	private LivroService livroService;

	@Autowired
	private CategoriaService categoriaService;

	@Autowired
	private AutorService autorService;

	@Autowired
	private ExemplarService exemplarService;

	@GetMapping
	public String listar(Model model) {
		model.addAttribute("livros", this.livroService.listar());
		return "livro/lista";
	}

	@GetMapping("/{id}")
	public String detalhe(@PathVariable Long id, Model model) {
		Livro livro = this.livroService.buscarPorId(id);
		model.addAttribute("livro", livro);
		model.addAttribute("exemplares", this.exemplarService.listarPorLivro(id));
		return "livro/detalhe";
	}

	@GetMapping("formulario")
	public String formulario(RequisicaoNovoLivro requisicao, Model model) {
		carregarOpcoes(model);
		return "livro/formulario";
	}

	@PostMapping("novo")
	public String novo(@Valid RequisicaoNovoLivro requisicao, BindingResult result, Model model) {
		if(result.hasErrors()) {
			carregarOpcoes(model);
			return "livro/formulario";
		}

		try {
			Livro livro = requisicao.toEntidade();
			this.livroService.cadastrar(livro);
		} catch (IllegalArgumentException | EntityNotFoundException exception) {
			result.reject("cadastro", exception.getMessage());
			carregarOpcoes(model);
			return "livro/formulario";
		}

		return "redirect:/home";
	}

	@GetMapping("/{id}/editar")
	public String editar(@PathVariable Long id, Model model) {
		Livro livro = this.livroService.buscarPorId(id);
		RequisicaoNovoLivro requisicao = new RequisicaoNovoLivro();
		requisicao.setTitulo(livro.getTitulo());
		requisicao.setIsbn(livro.getIsbn());
		requisicao.setAnoPublicacao(livro.getAnoPublicacao());
		requisicao.setCategoriaId(livro.getCategoria().getId());

		for (Autor autor : livro.getAutores()) {
			requisicao.getAutoresIds().add(autor.getId());
		}

		model.addAttribute("requisicaoNovoLivro", requisicao);
		model.addAttribute("id", id);
		carregarOpcoes(model);
		return "livro/formulario";
	}

	@PostMapping("/{id}")
	public String atualizar(@PathVariable Long id, @Valid RequisicaoNovoLivro requisicao,
			BindingResult result, Model model) {
		this.livroService.buscarPorId(id);
		model.addAttribute("id", id);

		if(result.hasErrors()) {
			carregarOpcoes(model);
			return "livro/formulario";
		}

		try {
			Livro livro = requisicao.toEntidade();
			this.livroService.atualizar(id, livro);
		} catch (IllegalArgumentException | EntityNotFoundException exception) {
			result.reject("atualizacao", exception.getMessage());
			carregarOpcoes(model);
			return "livro/formulario";
		}

		return "redirect:/home";
	}

	private void carregarOpcoes(Model model) {
		model.addAttribute("categorias", this.categoriaService.listar());
		model.addAttribute("autores", this.autorService.listar());
	}
}
