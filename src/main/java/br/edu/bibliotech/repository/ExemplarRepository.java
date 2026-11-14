package br.edu.bibliotech.repository;

import br.edu.bibliotech.model.Exemplar;
import br.edu.bibliotech.model.StatusExemplar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExemplarRepository extends JpaRepository<Exemplar, Long> {

    Optional<Exemplar> findByCodigoTombo(String codigoTombo);

    List<Exemplar> findByLivroId(Long livroId);

    List<Exemplar> findByLivroIdAndStatus(Long livroId, StatusExemplar status);

    long countByLivroIdAndStatus(Long livroId, StatusExemplar status);
}
