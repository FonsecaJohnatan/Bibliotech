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

import br.edu.bibliotech.dto.LivroResposta;
import br.edu.bibliotech.dto.RequisicaoNovoLivro;
import br.edu.bibliotech.model.Livro;
import br.edu.bibliotech.service.LivroService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/service/livros")
public class LivroRest {

	@Autowired
	private LivroService livroService;

	@GetMapping
	public ResponseEntity<List<LivroResposta>> listar() {
		List<Livro> livros = this.livroService.listar();
		List<LivroResposta> resposta = new ArrayList<>();

		for (Livro livro : livros) {
			resposta.add(LivroResposta.fromLivro(livro));
		}

		if (resposta.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok(resposta);
	}

	@GetMapping("/{id}")
	public ResponseEntity<LivroResposta> buscarPorId(@PathVariable Long id) {
		Livro livro = this.livroService.buscarPorId(id);
		return ResponseEntity.ok(LivroResposta.fromLivro(livro));
	}

	@PostMapping
	public ResponseEntity<LivroResposta> cadastrar(@Valid @RequestBody RequisicaoNovoLivro requisicao) {
		Livro livro = this.livroService.cadastrar(requisicao.toEntidade());
		LivroResposta resposta = LivroResposta.fromLivro(livro);

		return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
	}

	@PutMapping("/{id}")
	public ResponseEntity<LivroResposta> atualizar(@PathVariable Long id, @Valid @RequestBody RequisicaoNovoLivro requisicao) {
		Livro livro = this.livroService.atualizar(id, requisicao.toEntidade());
		return ResponseEntity.ok(LivroResposta.fromLivro(livro));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable Long id) {
		this.livroService.excluir(id);
		return ResponseEntity.noContent().build();
	}
}
