package br.edu.bibliotech.security;

import br.edu.bibliotech.model.Perfil;
import br.edu.bibliotech.model.Usuario;
import br.edu.bibliotech.repository.UsuarioRepository;
import br.edu.bibliotech.service.UsuarioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class AdministradorInicial implements ApplicationRunner {

    private final UsuarioRepository usuarios;
    private final UsuarioService servico;
    private final String email;
    private final String senha;

    public AdministradorInicial(UsuarioRepository usuarios, UsuarioService servico,
            @Value("${bibliotech.admin.email:}") String email,
            @Value("${bibliotech.admin.senha:}") String senha) {
        this.usuarios = usuarios;
        this.servico = servico;
        this.email = email;
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        if (email.isBlank() || senha.isBlank() || usuarios.existsByPerfil(Perfil.ADMIN)) {
            return;
        }

        String emailNormalizado = email.toLowerCase(Locale.ROOT);
        if (usuarios.findByEmail(emailNormalizado).isPresent()) {
            throw new IllegalStateException("O e-mail do administrador inicial já pertence a outro usuário");
        }

        Usuario administrador = new Usuario("Administrador", email, senha, Perfil.ADMIN);
        servico.cadastrar(administrador);
    }
}
