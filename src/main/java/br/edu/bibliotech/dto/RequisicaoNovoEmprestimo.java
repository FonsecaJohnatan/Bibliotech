package br.edu.bibliotech.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class RequisicaoNovoEmprestimo {

	@NotNull(message = "{validacao.emprestimo.usuario.obrigatorio}")
	@Positive(message = "{validacao.emprestimo.usuario.invalido}")
	private Long usuarioId;

	@NotNull(message = "{validacao.emprestimo.exemplar.obrigatorio}")
	@Positive(message = "{validacao.emprestimo.exemplar.invalido}")
	private Long exemplarId;

	public Long getUsuarioId() {
		return usuarioId;
	}

	public void setUsuarioId(Long usuarioId) {
		this.usuarioId = usuarioId;
	}

	public Long getExemplarId() {
		return exemplarId;
	}

	public void setExemplarId(Long exemplarId) {
		this.exemplarId = exemplarId;
	}
}
