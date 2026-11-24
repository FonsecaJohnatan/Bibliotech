package br.edu.bibliotech.dto;

import br.edu.bibliotech.model.Categoria;

public class CategoriaResposta {

	private Long id;
	private String nome;
	private String descricao;

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

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public static CategoriaResposta fromCategoria(Categoria categoria) {
		CategoriaResposta resposta = new CategoriaResposta();
		resposta.setId(categoria.getId());
		resposta.setNome(categoria.getNome());
		resposta.setDescricao(categoria.getDescricao());
		return resposta;
	}
}
