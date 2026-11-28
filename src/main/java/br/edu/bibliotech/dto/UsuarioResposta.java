package br.edu.bibliotech.dto;

import br.edu.bibliotech.model.Perfil;
import br.edu.bibliotech.model.Usuario;

public class UsuarioResposta {

	private Long id;

	private String nome;

	private String email;

	private Perfil perfil;

	private Boolean ativo;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

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

	public Perfil getPerfil() {
		return perfil;
	}

	public void setPerfil(Perfil perfil) {
		this.perfil = perfil;
	}

	public Boolean getAtivo() {
		return ativo;
	}

	public void setAtivo(Boolean ativo) {
		this.ativo = ativo;
	}

	public static UsuarioResposta fromUsuario(Usuario usuario) {
		UsuarioResposta resposta = new UsuarioResposta();

		resposta.setId(usuario.getId());
		resposta.setNome(usuario.getNome());
		resposta.setEmail(usuario.getEmail());
		resposta.setPerfil(usuario.getPerfil());
		resposta.setAtivo(usuario.getAtivo());

		return resposta;
	}
}
