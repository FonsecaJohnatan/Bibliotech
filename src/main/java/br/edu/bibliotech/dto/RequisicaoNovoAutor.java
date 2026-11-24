package br.edu.bibliotech.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import br.edu.bibliotech.model.Autor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RequisicaoNovoAutor {

	@NotBlank(message = "O nome do autor é obrigatório")
	@Size(max = 120, message = "O nome do autor deve ter no máximo 120 caracteres")
	private String nome;

	@Size(max = 60, message = "A nacionalidade deve ter no máximo 60 caracteres")
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

	public Autor toAutor() {
		Autor autor = new Autor();
		autor.setNome(this.nome);
		autor.setNacionalidade(this.nacionalidade);
		autor.setDataNascimento(this.dataNascimento);
		return autor;
	}
}
