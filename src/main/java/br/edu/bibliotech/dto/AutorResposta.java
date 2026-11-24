package br.edu.bibliotech.dto;

import java.time.LocalDate;

import br.edu.bibliotech.model.Autor;

public class AutorResposta {

	private Long id;
	private String nome;
	private String nacionalidade;
	private LocalDate dataNascimento;

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

	public String getNacionalidade() {
		return nacionalidade;
	}

	public void setNacionalidade(String nacionalidade) {
		this.nacionalidade = nacionalidade;
	}

	public LocalDate getDataNascimento() {
		return dataNascimento;
	}

	public void setDataNascimento(LocalDate dataNascimento) {
		this.dataNascimento = dataNascimento;
	}

	public static AutorResposta fromAutor(Autor autor) {
		AutorResposta resposta = new AutorResposta();
		resposta.setId(autor.getId());
		resposta.setNome(autor.getNome());
		resposta.setNacionalidade(autor.getNacionalidade());
		resposta.setDataNascimento(autor.getDataNascimento());
		return resposta;
	}
}
