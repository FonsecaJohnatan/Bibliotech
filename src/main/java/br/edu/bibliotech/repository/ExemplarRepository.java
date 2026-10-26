package br.edu.bibliotech.repository;

import br.edu.bibliotech.model.Exemplar;
import br.edu.bibliotech.model.StatusExemplar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExemplarRepository extends JpaRepository<Exemplar, Long> {

    List<Exemplar> findByLivroIdAndStatus(Long livroId, StatusExemplar status);

    long countByLivroIdAndStatus(Long livroId, StatusExemplar status);
}
