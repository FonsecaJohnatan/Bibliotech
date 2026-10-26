package br.edu.bibliotech.repository;

import br.edu.bibliotech.model.Emprestimo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EmprestimoRepository extends JpaRepository<Emprestimo, Long> {

    List<Emprestimo> findByUsuarioId(Long usuarioId);

    long countByUsuarioIdAndDataDevolucaoIsNull(Long usuarioId);

    List<Emprestimo> findByDataDevolucaoIsNullAndDataPrevistaBefore(LocalDate hoje);
}
