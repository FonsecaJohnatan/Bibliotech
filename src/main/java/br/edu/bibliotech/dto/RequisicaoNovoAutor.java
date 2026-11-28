package br.edu.bibliotech.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import br.edu.bibliotech.model.Autor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RequisicaoNovoAutor {

	@NotBlank(message = "{validacao.autor.nome.obrigatorio}")
	@Size(max = 120, message = "{validacao.autor.nome.tamanho}")
	private String nome;

	@Size(max = 60, message = "{validacao.autor.nacionalidade.tamanho}")
	private String nacionalidade;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate dataNascimento;

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

	public Autor toEntidade() {
		Autor autor = new Autor();
		autor.setNome(this.nome);
		autor.setNacionalidade(this.nacionalidade);
		autor.setDataNascimento(this.dataNascimento);
		return autor;
	}
}
