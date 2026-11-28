package br.edu.bibliotech.dto;

import br.edu.bibliotech.model.Exemplar;
import br.edu.bibliotech.model.Livro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class RequisicaoNovoExemplar {

	@NotBlank(message = "{validacao.exemplar.codigo.obrigatorio}")
	@Size(max = 20, message = "{validacao.exemplar.codigo.tamanho}")
	private String codigoTombo;

	@NotNull(message = "{validacao.exemplar.livro.obrigatorio}")
	@Positive(message = "{validacao.exemplar.livro.invalido}")
	private Long livroId;

	public String getCodigoTombo() {
		return codigoTombo;
	}

	public void setCodigoTombo(String codigoTombo) {
		this.codigoTombo = codigoTombo;
	}

	public Long getLivroId() {
		return livroId;
	}

	public void setLivroId(Long livroId) {
		this.livroId = livroId;
	}

	public Exemplar toEntidade() {
		Exemplar exemplar = new Exemplar();
		exemplar.setCodigoTombo(this.codigoTombo);

		Livro livro = new Livro();
		livro.setId(this.livroId);
		exemplar.setLivro(livro);

		return exemplar;
	}
}
