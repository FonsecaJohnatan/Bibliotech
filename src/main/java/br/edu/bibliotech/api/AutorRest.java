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
import org.springframework.web.bind.annotation.RestController;

import br.edu.bibliotech.dto.RequisicaoNovoAutor;
import br.edu.bibliotech.dto.AutorResposta;
import br.edu.bibliotech.model.Autor;
import br.edu.bibliotech.service.AutorService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/service/autores")
public class AutorRest {

	@Autowired
	private AutorService autorService;

	@GetMapping
	public ResponseEntity<List<AutorResposta>> listar() {
		List<Autor> autores = autorService.listar();
		List<AutorResposta> resposta = new ArrayList<>();

		for (Autor autor : autores) {
			resposta.add(AutorResposta.fromAutor(autor));
		}

		if (resposta.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok(resposta);
	}

	@GetMapping("/{id}")
	public ResponseEntity<AutorResposta> buscarPorId(@PathVariable Long id) {
		Autor autor = autorService.buscarPorId(id);
		return ResponseEntity.ok(AutorResposta.fromAutor(autor));
	}

	@PostMapping
	public ResponseEntity<AutorResposta> cadastrar(@Valid @RequestBody RequisicaoNovoAutor requisicao) {
		Autor autor = autorService.cadastrar(requisicao.toEntidade());
		AutorResposta resposta = AutorResposta.fromAutor(autor);

		return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
	}

	@PutMapping("/{id}")
	public ResponseEntity<AutorResposta> atualizar(@PathVariable Long id, @Valid @RequestBody RequisicaoNovoAutor requisicao) {
		Autor autor = autorService.atualizar(id, requisicao.toEntidade());
		return ResponseEntity.ok(AutorResposta.fromAutor(autor));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable Long id) {
		autorService.excluir(id);
		return ResponseEntity.noContent().build();
	}
}
