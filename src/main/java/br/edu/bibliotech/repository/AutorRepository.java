package br.edu.bibliotech.repository;

import br.edu.bibliotech.model.Autor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AutorRepository extends JpaRepository<Autor, Long> {

    List<Autor> findByNomeContainingIgnoreCase(String nome);
}
