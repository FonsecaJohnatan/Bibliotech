package br.edu.bibliotech.dto;

import br.edu.bibliotech.model.Exemplar;
import br.edu.bibliotech.model.StatusExemplar;

public class ExemplarResposta {

	private Long id;
	private String codigoTombo;
	private StatusExemplar status;
	private LivroResposta livro;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCodigoTombo() {
		return codigoTombo;
	}

	public void setCodigoTombo(String codigoTombo) {
		this.codigoTombo = codigoTombo;
	}

	public StatusExemplar getStatus() {
		return status;
	}

	public void setStatus(StatusExemplar status) {
		this.status = status;
	}

	public LivroResposta getLivro() {
		return livro;
	}

	public void setLivro(LivroResposta livro) {
		this.livro = livro;
	}

	public static ExemplarResposta fromExemplar(Exemplar exemplar) {
		ExemplarResposta resposta = new ExemplarResposta();

		resposta.setId(exemplar.getId());
		resposta.setCodigoTombo(exemplar.getCodigoTombo());
		resposta.setStatus(exemplar.getStatus());
		resposta.setLivro(LivroResposta.fromLivro(exemplar.getLivro()));

		return resposta;
	}
}
