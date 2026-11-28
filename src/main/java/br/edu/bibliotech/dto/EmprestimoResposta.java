package br.edu.bibliotech.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.edu.bibliotech.model.Emprestimo;

public class EmprestimoResposta {

	private Long id;
	private LocalDate dataEmprestimo;
	private LocalDate dataPrevista;
	private LocalDate dataDevolucao;
	private Boolean renovado;
	private BigDecimal multa;
	private UsuarioResposta usuario;
	private ExemplarResposta exemplar;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public LocalDate getDataEmprestimo() {
		return dataEmprestimo;
	}

	public void setDataEmprestimo(LocalDate dataEmprestimo) {
		this.dataEmprestimo = dataEmprestimo;
	}

	public LocalDate getDataPrevista() {
		return dataPrevista;
	}

	public void setDataPrevista(LocalDate dataPrevista) {
		this.dataPrevista = dataPrevista;
	}

	public LocalDate getDataDevolucao() {
		return dataDevolucao;
	}

	public void setDataDevolucao(LocalDate dataDevolucao) {
		this.dataDevolucao = dataDevolucao;
	}

	public Boolean getRenovado() {
		return renovado;
	}

	public void setRenovado(Boolean renovado) {
		this.renovado = renovado;
	}

	public BigDecimal getMulta() {
		return multa;
	}

	public void setMulta(BigDecimal multa) {
		this.multa = multa;
	}

	public UsuarioResposta getUsuario() {
		return usuario;
	}

	public void setUsuario(UsuarioResposta usuario) {
		this.usuario = usuario;
	}

	public ExemplarResposta getExemplar() {
		return exemplar;
	}

	public void setExemplar(ExemplarResposta exemplar) {
		this.exemplar = exemplar;
	}

	public static EmprestimoResposta fromEmprestimo(Emprestimo emprestimo) {
		EmprestimoResposta resposta = new EmprestimoResposta();

		resposta.setId(emprestimo.getId());
		resposta.setDataEmprestimo(emprestimo.getDataEmprestimo());
		resposta.setDataPrevista(emprestimo.getDataPrevista());
		resposta.setDataDevolucao(emprestimo.getDataDevolucao());
		resposta.setRenovado(emprestimo.getRenovado());
		resposta.setMulta(emprestimo.getMulta());
		resposta.setUsuario(UsuarioResposta.fromUsuario(emprestimo.getUsuario()));
		resposta.setExemplar(ExemplarResposta.fromExemplar(emprestimo.getExemplar()));

		return resposta;
	}
}
