package br.edu.bibliotech.repository;

import br.edu.bibliotech.model.Livro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LivroRepository extends JpaRepository<Livro, Long> {

    Page<Livro> findByTituloContainingIgnoreCase(String titulo, Pageable pageable);

    Page<Livro> findByCategoriaId(Long categoriaId, Pageable pageable);

    Page<Livro> findByAutoresId(Long autorId, Pageable pageable);

    Optional<Livro> findByIsbn(String isbn);
}
