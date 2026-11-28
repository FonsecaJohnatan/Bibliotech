package br.edu.bibliotech.api;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.bibliotech.dto.EmprestimoResposta;
import br.edu.bibliotech.dto.RequisicaoNovoEmprestimo;
import br.edu.bibliotech.model.Emprestimo;
import br.edu.bibliotech.service.EmprestimoService;
import br.edu.bibliotech.service.UsuarioService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/service/emprestimos")
public class EmprestimoRest {

	@Autowired
	private EmprestimoService emprestimoService;

	@Autowired
	private UsuarioService usuarioService;

	@GetMapping
	public ResponseEntity<List<EmprestimoResposta>> listar(@RequestParam(required = false) Long usuarioId) {
		List<Emprestimo> emprestimos;

		if (usuarioId == null) {
			emprestimos = this.emprestimoService.listar();
		} else {
			this.usuarioService.buscarPorId(usuarioId);
			emprestimos = this.emprestimoService.listarPorUsuario(usuarioId);
		}

		List<EmprestimoResposta> resposta = new ArrayList<>();

		for (Emprestimo emprestimo : emprestimos) {
			resposta.add(EmprestimoResposta.fromEmprestimo(emprestimo));
		}

		if (resposta.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok(resposta);
	}

	@GetMapping("/{id}")
	public ResponseEntity<EmprestimoResposta> buscarPorId(@PathVariable Long id) {
		Emprestimo emprestimo = this.emprestimoService.buscarPorId(id);
		return ResponseEntity.ok(EmprestimoResposta.fromEmprestimo(emprestimo));
	}

	@PostMapping
	public ResponseEntity<EmprestimoResposta> cadastrar(@Valid @RequestBody RequisicaoNovoEmprestimo requisicao) {
		Emprestimo emprestimo = this.emprestimoService.registrar(requisicao.getUsuarioId(), requisicao.getExemplarId());
		EmprestimoResposta resposta = EmprestimoResposta.fromEmprestimo(emprestimo);

		return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
	}

	@PutMapping("/{id}/renovar")
	public ResponseEntity<EmprestimoResposta> renovar(@PathVariable Long id) {
		Emprestimo emprestimo = this.emprestimoService.renovar(id);
		return ResponseEntity.ok(EmprestimoResposta.fromEmprestimo(emprestimo));
	}

	@PutMapping("/{id}/devolver")
	public ResponseEntity<EmprestimoResposta> devolver(@PathVariable Long id) {
		Emprestimo emprestimo = this.emprestimoService.devolver(id);
		return ResponseEntity.ok(EmprestimoResposta.fromEmprestimo(emprestimo));
	}
}
