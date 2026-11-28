package br.edu.bibliotech.dto;

import br.edu.bibliotech.model.Categoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RequisicaoNovaCategoria {

	@NotBlank(message = "{validacao.categoria.nome.obrigatorio}")
	@Size(max = 80, message = "{validacao.categoria.nome.tamanho}")
	private String nome;

	@Size(max = 255, message = "{validacao.categoria.descricao.tamanho}")
	private String descricao;

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

	public Categoria toEntidade() {
		Categoria categoria = new Categoria();
		categoria.setNome(this.nome);
		categoria.setDescricao(this.descricao);
		return categoria;
	}
}
