package br.edu.bibliotech.dto;

import br.edu.bibliotech.model.Perfil;
import br.edu.bibliotech.model.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RequisicaoNovoUsuario {

	@NotBlank(message = "{validacao.usuario.nome.obrigatorio}")
	@Size(max = 120, message = "{validacao.usuario.nome.tamanho}")
	private String nome;

	@NotBlank(message = "{validacao.usuario.email.obrigatorio}")
	@Email(message = "{validacao.usuario.email.formato}")
	@Size(max = 150, message = "{validacao.usuario.email.tamanho}")
	private String email;

	@NotBlank(message = "{validacao.usuario.senha.obrigatoria}")
	private String senha;

	@NotNull(message = "{validacao.usuario.perfil.obrigatorio}")
	private Perfil perfil;

    private boolean ativo = true;

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

	public String getSenha() {
		return senha;
	}

	public void setSenha(String senha) {
		this.senha = senha;
	}

	public Perfil getPerfil() {
		return perfil;
	}

	public void setPerfil(Perfil perfil) {
		this.perfil = perfil;
	}

	public Usuario toEntidade() {
		Usuario usuario = new Usuario();
		usuario.setNome(this.nome);
		usuario.setEmail(this.email);
		usuario.setSenha(this.senha);
		usuario.setPerfil(this.perfil);
        usuario.setAtivo(this.ativo);
		return usuario;
	}

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

}
