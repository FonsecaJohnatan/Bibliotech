package br.edu.bibliotech.dto;

import br.edu.bibliotech.model.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RequisicaoAtualizarUsuario {

	@NotBlank(message = "{validacao.usuario.nome.obrigatorio}")
	@Size(max = 120, message = "{validacao.usuario.nome.tamanho}")
	private String nome;

	@NotBlank(message = "{validacao.usuario.email.obrigatorio}")
	@Email(message = "{validacao.usuario.email.formato}")
	@Size(max = 150, message = "{validacao.usuario.email.tamanho}")
	private String email;

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Usuario toEntidade() {
		Usuario usuario = new Usuario();
		usuario.setNome(this.nome);
		usuario.setEmail(this.email);
		return usuario;
	}
}
