package br.edu.bibliotech.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "emprestimos")
@Getter
@Setter
@NoArgsConstructor
public class Emprestimo {

    public static final int PRAZO_DIAS = 14;
    public static final int RENOVACAO_DIAS = 7;
    public static final BigDecimal MULTA_POR_DIA = new BigDecimal("1.00");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_emprestimo", nullable = false)
    private LocalDate dataEmprestimo;

    @Column(name = "data_prevista", nullable = false)
    private LocalDate dataPrevista;

    @Column(name = "data_devolucao")
    private LocalDate dataDevolucao;

    @Column(nullable = false)
    private Boolean renovado = false;

    @Column(precision = 8, scale = 2)
    private BigDecimal multa = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exemplar_id", nullable = false)
    private Exemplar exemplar;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    public Emprestimo(Exemplar exemplar, Usuario usuario, LocalDate dataEmprestimo) {
        this.exemplar = exemplar;
        this.usuario = usuario;
        this.dataEmprestimo = dataEmprestimo;
        this.dataPrevista = dataEmprestimo.plusDays(PRAZO_DIAS);
    }

    public void renovar() {
        if (Boolean.TRUE.equals(renovado)) {
            throw new IllegalStateException("Empréstimo já foi renovado uma vez");
        }
        if (dataDevolucao != null) {
            throw new IllegalStateException("Empréstimo já foi devolvido");
        }
        this.dataPrevista = dataPrevista.plusDays(RENOVACAO_DIAS);
        this.renovado = true;
    }

    public BigDecimal calcularMulta(LocalDate dataReferencia) {
        long diasAtraso = ChronoUnit.DAYS.between(dataPrevista, dataReferencia);
        if (diasAtraso <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return MULTA_POR_DIA.multiply(BigDecimal.valueOf(diasAtraso));
    }
}
