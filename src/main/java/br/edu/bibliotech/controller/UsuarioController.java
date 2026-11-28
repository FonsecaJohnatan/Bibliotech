package br.edu.bibliotech.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import br.edu.bibliotech.dto.RequisicaoAtualizarUsuario;
import br.edu.bibliotech.dto.RequisicaoNovoUsuario;
import br.edu.bibliotech.model.Perfil;
import br.edu.bibliotech.model.Usuario;
import br.edu.bibliotech.service.UsuarioService;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/usuario")
public class UsuarioController {

	@Autowired
	private UsuarioService usuarioService;

	@GetMapping
	public String listar(Model model) {
		model.addAttribute("usuarios", this.usuarioService.listar());
		return "usuario/lista";
	}

	@GetMapping("formulario")
	public String formulario(RequisicaoNovoUsuario requisicao, Model model) {
		model.addAttribute("perfis", Perfil.values());
		return "usuario/formulario";
	}

	@PostMapping("novo")
	public String novo(@Valid RequisicaoNovoUsuario requisicao, BindingResult result, Model model) {
		if(result.hasErrors()) {
			model.addAttribute("perfis", Perfil.values());
			return "usuario/formulario";
		}

		try {
			Usuario usuario = requisicao.toEntidade();
			this.usuarioService.cadastrar(usuario);
		} catch (IllegalArgumentException exception) {
			result.reject("cadastro", exception.getMessage());
			model.addAttribute("perfis", Perfil.values());
			return "usuario/formulario";
		}

		return "redirect:/home";
	}

	@GetMapping("/{id}/editar")
	public String editar(@PathVariable Long id, Model model) {
		Usuario usuario = this.usuarioService.buscarPorId(id);
		RequisicaoAtualizarUsuario requisicao = new RequisicaoAtualizarUsuario();
		requisicao.setNome(usuario.getNome());
		requisicao.setEmail(usuario.getEmail());

		model.addAttribute("requisicaoAtualizarUsuario", requisicao);
		model.addAttribute("id", id);
		return "usuario/edicao";
	}

	@PostMapping("/{id}")
	public String atualizar(@PathVariable Long id, @Valid RequisicaoAtualizarUsuario requisicao,
			BindingResult result, Model model) {
		this.usuarioService.buscarPorId(id);
		model.addAttribute("id", id);

		if(result.hasErrors()) {
			return "usuario/edicao";
		}

		try {
			Usuario usuario = requisicao.toEntidade();
			this.usuarioService.atualizar(id, usuario);
		} catch (IllegalArgumentException exception) {
			result.reject("atualizacao", exception.getMessage());
			return "usuario/edicao";
		}

		return "redirect:/home";
	}

	@PostMapping("/{id}/desativar")
	public String desativar(@PathVariable Long id) {
		this.usuarioService.desativar(id);
		return "redirect:/home";
	}
}
