package br.edu.bibliotech.repository;

import br.edu.bibliotech.model.Usuario;
import br.edu.bibliotech.model.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByPerfil(Perfil perfil);
}
