package br.edu.bibliotech.controller;

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

import br.edu.bibliotech.dto.RequisicaoNovoExemplar;
import br.edu.bibliotech.model.Exemplar;
import br.edu.bibliotech.model.StatusExemplar;
import java.util.List;
import br.edu.bibliotech.service.ExemplarService;
import br.edu.bibliotech.service.LivroService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/exemplar")
public class ExemplarController {

	@Autowired
	private ExemplarService exemplarService;

	@Autowired
	private LivroService livroService;

	@GetMapping
	public String listar(@RequestParam(required = false) Long livroId,
            @RequestParam(required = false) StatusExemplar status, Model model) {
        List<Exemplar> exemplares = livroId == null ? exemplarService.listar() : exemplarService.listarPorLivro(livroId);
        model.addAttribute("exemplares", exemplares.stream()
                .filter(exemplar -> status == null || exemplar.getStatus() == status)
                .toList());
        model.addAttribute("status", status);

		model.addAttribute("livroId", livroId);
		carregarOpcoes(model);
		return "exemplar/lista";
	}

	@GetMapping("formulario")
	public String formulario(RequisicaoNovoExemplar requisicao, Model model) {
		carregarOpcoes(model);
		return "exemplar/formulario";
	}

	@PostMapping("novo")
	public String novo(@Valid RequisicaoNovoExemplar requisicao, BindingResult result, Model model) {
		if(result.hasErrors()) {
			carregarOpcoes(model);
			return "exemplar/formulario";
		}

		try {
			Exemplar exemplar = requisicao.toEntidade();
			this.exemplarService.cadastrar(exemplar, requisicao.getStatus());
		} catch (IllegalArgumentException | EntityNotFoundException exception) {
			result.reject("cadastro", exception.getMessage());
			carregarOpcoes(model);
			return "exemplar/formulario";
		}

		return "redirect:/exemplar";
	}

	@GetMapping("/{id}/editar")
	public String editar(@PathVariable Long id, Model model) {
		Exemplar exemplar = this.exemplarService.buscarPorId(id);
		RequisicaoNovoExemplar requisicao = new RequisicaoNovoExemplar();
		requisicao.setCodigoTombo(exemplar.getCodigoTombo());
        requisicao.setStatus(exemplar.getStatus());
		requisicao.setLivroId(exemplar.getLivro().getId());

		model.addAttribute("requisicaoNovoExemplar", requisicao);
		model.addAttribute("id", id);
		carregarOpcoes(model);
		return "exemplar/formulario";
	}

	@PostMapping("/{id}")
	public String atualizar(@PathVariable Long id, @Valid RequisicaoNovoExemplar requisicao,
			BindingResult result, Model model) {
		this.exemplarService.buscarPorId(id);
		model.addAttribute("id", id);

		if(result.hasErrors()) {
			carregarOpcoes(model);
			return "exemplar/formulario";
		}

		try {
			Exemplar exemplar = requisicao.toEntidade();
			this.exemplarService.atualizar(id, exemplar, requisicao.getStatus());
		} catch (IllegalArgumentException | IllegalStateException | EntityNotFoundException exception) {
			result.reject("atualizacao", exception.getMessage());
			carregarOpcoes(model);
			return "exemplar/formulario";
		}

		return "redirect:/exemplar";
	}

	@PostMapping("/{id}/excluir")
	public String excluir(@PathVariable Long id, RedirectAttributes atributos) {
		try {
			this.exemplarService.excluir(id);
		} catch (IllegalStateException exception) {
			atributos.addFlashAttribute("erro", exception.getMessage());
			return "redirect:/exemplar";
		}

		return "redirect:/exemplar";
	}

	private void carregarOpcoes(Model model) {
		model.addAttribute("livros", this.livroService.listar());
        model.addAttribute("statusExemplares", StatusExemplar.values());
	}
}
