package br.edu.bibliotech.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.edu.bibliotech.dto.RequisicaoNovoEmprestimo;
import br.edu.bibliotech.model.Exemplar;
import br.edu.bibliotech.model.StatusExemplar;
import br.edu.bibliotech.model.Usuario;
import br.edu.bibliotech.service.EmprestimoService;
import br.edu.bibliotech.service.ExemplarService;
import br.edu.bibliotech.service.UsuarioService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/emprestimo")
public class EmprestimoController {

	@Autowired
	private EmprestimoService emprestimoService;

	@Autowired
	private UsuarioService usuarioService;

	@Autowired
	private ExemplarService exemplarService;

	@GetMapping
	public String listar(@RequestParam(required = false) Long usuarioId, Model model) {
		if (usuarioId == null) {
			model.addAttribute("emprestimos", this.emprestimoService.listar());
		} else {
			this.usuarioService.buscarPorId(usuarioId);
			model.addAttribute("emprestimos", this.emprestimoService.listarPorUsuario(usuarioId));
		}

		model.addAttribute("usuarioId", usuarioId);
		model.addAttribute("usuarios", this.usuarioService.listar());
		return "emprestimo/lista";
	}

	@GetMapping("formulario")
	public String formulario(RequisicaoNovoEmprestimo requisicao, Model model) {
		carregarOpcoes(model);
		return "emprestimo/formulario";
	}

	@PostMapping("novo")
	public String novo(@Valid RequisicaoNovoEmprestimo requisicao, BindingResult result, Model model) {
		if(result.hasErrors()) {
			carregarOpcoes(model);
			return "emprestimo/formulario";
		}

		try {
			this.emprestimoService.registrar(requisicao.getUsuarioId(), requisicao.getExemplarId());
		} catch (IllegalArgumentException | IllegalStateException | EntityNotFoundException exception) {
			result.reject("cadastro", exception.getMessage());
			carregarOpcoes(model);
			return "emprestimo/formulario";
		}

		return "redirect:/home";
	}

	@PostMapping("/{id}/renovar")
	public String renovar(@PathVariable Long id, RedirectAttributes atributos) {
		try {
			this.emprestimoService.renovar(id);
		} catch (IllegalStateException exception) {
			atributos.addFlashAttribute("erro", exception.getMessage());
			return "redirect:/emprestimo";
		}

		return "redirect:/home";
	}

	@PostMapping("/{id}/devolver")
	public String devolver(@PathVariable Long id, RedirectAttributes atributos) {
		try {
			this.emprestimoService.devolver(id);
		} catch (IllegalArgumentException | IllegalStateException exception) {
			atributos.addFlashAttribute("erro", exception.getMessage());
			return "redirect:/emprestimo";
		}

		return "redirect:/home";
	}

	@GetMapping("/{id}/devolucao")
	public String confirmarDevolucao(@PathVariable Long id, Model model) {
		model.addAttribute("emprestimo", emprestimoService.buscarPorId(id));
		return "emprestimo/devolucao";
	}

	private void carregarOpcoes(Model model) {
		List<Usuario> usuarios = new ArrayList<>();
		List<Exemplar> exemplares = new ArrayList<>();

		for (Usuario usuario : this.usuarioService.listar()) {
			if (Boolean.TRUE.equals(usuario.getAtivo())) {
				usuarios.add(usuario);
			}
		}

		for (Exemplar exemplar : this.exemplarService.listar()) {
			if (exemplar.getStatus() == StatusExemplar.DISPONIVEL) {
				exemplares.add(exemplar);
			}
		}

		model.addAttribute("usuarios", usuarios);
		model.addAttribute("exemplares", exemplares);
	}
}
