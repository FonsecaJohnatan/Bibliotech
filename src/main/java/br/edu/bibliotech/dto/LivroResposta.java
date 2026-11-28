package br.edu.bibliotech.dto;

import java.util.ArrayList;
import java.util.List;

import br.edu.bibliotech.model.Autor;
import br.edu.bibliotech.model.Livro;

public class LivroResposta {

	private Long id;
	private String titulo;
	private String isbn;
	private Integer anoPublicacao;
	private CategoriaResposta categoria;
	private List<AutorResposta> autores = new ArrayList<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

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

	public CategoriaResposta getCategoria() {
		return categoria;
	}

	public void setCategoria(CategoriaResposta categoria) {
		this.categoria = categoria;
	}

	public List<AutorResposta> getAutores() {
		return autores;
	}

	public void setAutores(List<AutorResposta> autores) {
		this.autores = autores;
	}

	public static LivroResposta fromLivro(Livro livro) {
		LivroResposta resposta = new LivroResposta();

		resposta.setId(livro.getId());
		resposta.setTitulo(livro.getTitulo());
		resposta.setIsbn(livro.getIsbn());
		resposta.setAnoPublicacao(livro.getAnoPublicacao());
		resposta.setCategoria(CategoriaResposta.fromCategoria(livro.getCategoria()));

		for (Autor autor : livro.getAutores()) {
			resposta.getAutores().add(AutorResposta.fromAutor(autor));
		}

		return resposta;
	}
}
