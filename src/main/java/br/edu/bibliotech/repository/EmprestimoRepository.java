package br.edu.bibliotech.repository;

import br.edu.bibliotech.model.Emprestimo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmprestimoRepository extends JpaRepository<Emprestimo, Long> {

    @Override
    @EntityGraph(attributePaths = {"usuario", "exemplar.livro.categoria", "exemplar.livro.autores"})
    List<Emprestimo> findAll();

    @Override
    @EntityGraph(attributePaths = {"usuario", "exemplar.livro.categoria", "exemplar.livro.autores"})
    Optional<Emprestimo> findById(Long id);

    boolean existsByExemplarId(Long exemplarId);

    @EntityGraph(attributePaths = {"usuario", "exemplar.livro.categoria", "exemplar.livro.autores"})
    List<Emprestimo> findByUsuarioId(Long usuarioId);

    long countByUsuarioIdAndDataDevolucaoIsNull(Long usuarioId);

    List<Emprestimo> findByDataDevolucaoIsNullAndDataPrevistaBefore(LocalDate hoje);
}
