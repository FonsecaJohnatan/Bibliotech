package br.edu.bibliotech.repository;

import br.edu.bibliotech.model.Exemplar;
import br.edu.bibliotech.model.StatusExemplar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface ExemplarRepository extends JpaRepository<Exemplar, Long> {

    @Override
    @EntityGraph(attributePaths = {"livro.categoria", "livro.autores"})
    List<Exemplar> findAll();

    @Override
    @EntityGraph(attributePaths = {"livro.categoria", "livro.autores"})
    Optional<Exemplar> findById(Long id);

    Optional<Exemplar> findByCodigoTombo(String codigoTombo);

    @EntityGraph(attributePaths = {"livro.categoria", "livro.autores"})
    List<Exemplar> findByLivroId(Long livroId);

    List<Exemplar> findByLivroIdAndStatus(Long livroId, StatusExemplar status);

    long countByLivroIdAndStatus(Long livroId, StatusExemplar status);
}
