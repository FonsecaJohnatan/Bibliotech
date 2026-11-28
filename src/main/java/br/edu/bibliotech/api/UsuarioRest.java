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

import br.edu.bibliotech.dto.RequisicaoAtualizarUsuario;
import br.edu.bibliotech.dto.RequisicaoNovoUsuario;
import br.edu.bibliotech.dto.UsuarioResposta;
import br.edu.bibliotech.model.Usuario;
import br.edu.bibliotech.service.UsuarioService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/service/usuarios")
public class UsuarioRest {

	@Autowired
	private UsuarioService usuarioService;

	@GetMapping
	public ResponseEntity<List<UsuarioResposta>> listar() {
		List<Usuario> usuarios = this.usuarioService.listar();
		List<UsuarioResposta> resposta = new ArrayList<>();

		for (Usuario usuario : usuarios) {
			resposta.add(UsuarioResposta.fromUsuario(usuario));
		}

		if (resposta.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok(resposta);
	}

	@GetMapping("/{id}")
	public ResponseEntity<UsuarioResposta> buscarPorId(@PathVariable Long id) {
		Usuario usuario = this.usuarioService.buscarPorId(id);
		return ResponseEntity.ok(UsuarioResposta.fromUsuario(usuario));
	}

	@PostMapping
	public ResponseEntity<UsuarioResposta> cadastrar(@Valid @RequestBody RequisicaoNovoUsuario requisicao) {
		Usuario usuario = this.usuarioService.cadastrar(requisicao.toEntidade());
		UsuarioResposta resposta = UsuarioResposta.fromUsuario(usuario);

		return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
	}

	@PutMapping("/{id}")
	public ResponseEntity<UsuarioResposta> atualizar(@PathVariable Long id, @Valid @RequestBody RequisicaoAtualizarUsuario requisicao) {
		Usuario usuario = this.usuarioService.atualizar(id, requisicao.toEntidade());
		return ResponseEntity.ok(UsuarioResposta.fromUsuario(usuario));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> desativar(@PathVariable Long id) {
		this.usuarioService.desativar(id);
		return ResponseEntity.noContent().build();
	}
}
