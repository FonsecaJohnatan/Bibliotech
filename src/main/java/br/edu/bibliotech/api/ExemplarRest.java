package br.edu.bibliotech.api;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.bibliotech.dto.ExemplarResposta;
import br.edu.bibliotech.dto.RequisicaoNovoExemplar;
import br.edu.bibliotech.model.Exemplar;
import br.edu.bibliotech.service.ExemplarService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/service/exemplares")
public class ExemplarRest {

	@Autowired
	private ExemplarService exemplarService;

	@GetMapping
	public ResponseEntity<List<ExemplarResposta>> listar(@RequestParam(required = false) Long livroId) {
		List<Exemplar> exemplares;

		if (livroId == null) {
			exemplares = this.exemplarService.listar();
		} else {
			exemplares = this.exemplarService.listarPorLivro(livroId);
		}

		List<ExemplarResposta> resposta = new ArrayList<>();

		for (Exemplar exemplar : exemplares) {
			resposta.add(ExemplarResposta.fromExemplar(exemplar));
		}

		if (resposta.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok(resposta);
	}

	@GetMapping("/{id}")
	public ResponseEntity<ExemplarResposta> buscarPorId(@PathVariable Long id) {
		Exemplar exemplar = this.exemplarService.buscarPorId(id);
		return ResponseEntity.ok(ExemplarResposta.fromExemplar(exemplar));
	}

	@PostMapping
	public ResponseEntity<ExemplarResposta> cadastrar(@Valid @RequestBody RequisicaoNovoExemplar requisicao) {
		Exemplar exemplar = this.exemplarService.cadastrar(requisicao.toEntidade());
		ExemplarResposta resposta = ExemplarResposta.fromExemplar(exemplar);

		return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
	}

	@PutMapping("/{id}")
	public ResponseEntity<ExemplarResposta> atualizar(@PathVariable Long id, @Valid @RequestBody RequisicaoNovoExemplar requisicao) {
		Exemplar exemplar = this.exemplarService.atualizar(id, requisicao.toEntidade());
		return ResponseEntity.ok(ExemplarResposta.fromExemplar(exemplar));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable Long id) {
		this.exemplarService.excluir(id);
		return ResponseEntity.noContent().build();
	}
}
