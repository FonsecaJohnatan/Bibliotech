package br.edu.bibliotech.dto;

import br.edu.bibliotech.model.Categoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RequisicaoNovaCategoria {

	@NotBlank(message = "O nome da categoria é obrigatório")
	@Size(max = 80, message = "O nome da categoria deve ter no máximo 80 caracteres")
	private String nome;

	@Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
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

	public Categoria toCategoria() {
		Categoria categoria = new Categoria();
		categoria.setNome(this.nome);
		categoria.setDescricao(this.descricao);
		return categoria;
	}
}
