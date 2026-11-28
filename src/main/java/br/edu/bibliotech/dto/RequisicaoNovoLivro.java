package br.edu.bibliotech.dto;

import java.util.HashSet;
import java.util.Set;

import br.edu.bibliotech.model.Autor;
import br.edu.bibliotech.model.Categoria;
import br.edu.bibliotech.model.Livro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class RequisicaoNovoLivro {

	@NotBlank(message = "{validacao.livro.titulo.obrigatorio}")
	@Size(max = 200, message = "{validacao.livro.titulo.tamanho}")
	private String titulo;

	@NotBlank(message = "{validacao.livro.isbn.obrigatorio}")
	private String isbn;

	private Integer anoPublicacao;

	@NotNull(message = "{validacao.livro.categoria.obrigatoria}")
	@Positive(message = "{validacao.livro.categoria.invalida}")
	private Long categoriaId;

	private Set<@NotNull @Positive Long> autoresIds = new HashSet<>();

	public String getTitulo() {
		return titulo;
	}

	public void setTitulo(String titulo) {
		this.titulo = titulo;
	}

	public String getIsbn() {
		return isbn;
	}

	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}

	public Integer getAnoPublicacao() {
		return anoPublicacao;
	}

	public void setAnoPublicacao(Integer anoPublicacao) {
		this.anoPublicacao = anoPublicacao;
	}

	public Long getCategoriaId() {
		return categoriaId;
	}

	public void setCategoriaId(Long categoriaId) {
		this.categoriaId = categoriaId;
	}

	public Set<Long> getAutoresIds() {
		return autoresIds;
	}

	public void setAutoresIds(Set<Long> autoresIds) {
		this.autoresIds = autoresIds;
	}

	public Livro toEntidade() {
		Livro livro = new Livro();
		livro.setTitulo(this.titulo);
		livro.setIsbn(this.isbn);
		livro.setAnoPublicacao(this.anoPublicacao);

		Categoria categoria = new Categoria();
		categoria.setId(this.categoriaId);
		livro.setCategoria(categoria);

		if (this.autoresIds != null) {
			for (Long id : this.autoresIds) {
				Autor autor = new Autor();
				
				autor.setId(id);
				livro.getAutores().add(autor);
			}
		}

		return livro;
	}
}
