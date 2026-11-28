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

import br.edu.bibliotech.dto.RequisicaoNovaCategoria;
import br.edu.bibliotech.dto.CategoriaResposta;
import br.edu.bibliotech.model.Categoria;
import br.edu.bibliotech.service.CategoriaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/service/categorias")
public class CategoriaRest {

	@Autowired
	private CategoriaService categoriaService;

	@GetMapping
	public ResponseEntity<List<CategoriaResposta>> listar() {
		List<Categoria> categorias = categoriaService.listar();
		List<CategoriaResposta> resposta = new ArrayList<>();

		for (Categoria categoria : categorias) {
			resposta.add(CategoriaResposta.fromCategoria(categoria));
		}

		if (resposta.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok(resposta);
	}

	@GetMapping("/{id}")
	public ResponseEntity<CategoriaResposta> buscarPorId(@PathVariable Long id) {
		Categoria categoria = categoriaService.buscarPorId(id);
		return ResponseEntity.ok(CategoriaResposta.fromCategoria(categoria));
	}

	@PostMapping
	public ResponseEntity<CategoriaResposta> cadastrar(@Valid @RequestBody RequisicaoNovaCategoria requisicao) {
		Categoria categoria = categoriaService.cadastrar(requisicao.toEntidade());
		CategoriaResposta resposta = CategoriaResposta.fromCategoria(categoria);

		return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
	}

	@PutMapping("/{id}")
	public ResponseEntity<CategoriaResposta> atualizar(@PathVariable Long id, @Valid @RequestBody RequisicaoNovaCategoria requisicao) {
		Categoria categoria = categoriaService.atualizar(id, requisicao.toEntidade());
		return ResponseEntity.ok(CategoriaResposta.fromCategoria(categoria));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable Long id) {
		categoriaService.excluir(id);
		return ResponseEntity.noContent().build();
	}
}
