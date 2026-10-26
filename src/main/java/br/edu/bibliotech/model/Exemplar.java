package br.edu.bibliotech.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exemplares")
@Getter
@Setter
@NoArgsConstructor
public class Exemplar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 20)
    @Column(name = "codigo_tombo", nullable = false, unique = true, length = 20)
    private String codigoTombo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusExemplar status = StatusExemplar.DISPONIVEL;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "livro_id", nullable = false)
    private Livro livro;

    @OneToMany(mappedBy = "exemplar")
    private List<Emprestimo> emprestimos = new ArrayList<>();

    public Exemplar(String codigoTombo, Livro livro) {
        this.codigoTombo = codigoTombo;
        this.livro = livro;
    }

    public void emprestar() {
        if (status != StatusExemplar.DISPONIVEL) {
            throw new IllegalStateException("Exemplar não está disponível para empréstimo");
        }
        this.status = StatusExemplar.EMPRESTADO;
    }

    public void devolver() {
        this.status = StatusExemplar.DISPONIVEL;
    }
}
