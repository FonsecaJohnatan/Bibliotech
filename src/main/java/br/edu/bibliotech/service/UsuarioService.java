package br.edu.bibliotech.service;

import br.edu.bibliotech.model.Perfil;
import br.edu.bibliotech.model.Usuario;
import br.edu.bibliotech.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class UsuarioService {

    private static final Pattern EMAIL_VALIDO = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario cadastrar(Usuario dados) {
        validarCadastro(dados);

        String nome = dados.getNome().strip();
        String email = normalizarEmail(dados.getEmail());
        String senha = dados.getSenha();
        Perfil perfil = dados.getPerfil();

        if (validarEmail(email, null)) {
            throw new IllegalArgumentException("E-mail já cadastrado");
        }

        Usuario usuario = new Usuario(nome, email, passwordEncoder.encode(senha), perfil);

        return usuarioRepository.save(usuario);
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + id));
    }

    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    public Usuario atualizar(Long id, Usuario dados) {
        Usuario usuario = buscarPorId(id);

        validarDadosCliente(dados);

        String nome = dados.getNome().strip();
        String email = normalizarEmail(dados.getEmail());

        if (validarEmail(email, id)) {
            throw new IllegalArgumentException("E-mail já cadastrado");
        }

        usuario.setNome(nome);
        usuario.setEmail(email);

        return usuarioRepository.save(usuario);
    }

    public Usuario alterarSenha(Long id, String senhaAtual, String novaSenha) {
        Usuario usuario = buscarPorId(id);
        String hashAtual = usuario.getSenha();

        if (senhaAtual == null || !passwordEncoder.matches(senhaAtual, hashAtual)) {
            throw new IllegalArgumentException("Senha atual incorreta");
        }
        if (novaSenha == null || novaSenha.isBlank()) {
            throw new IllegalArgumentException("Nova senha obrigatória");
        }

        String novoHash = passwordEncoder.encode(novaSenha);
        usuario.setSenha(novoHash);
        return usuarioRepository.save(usuario);
    }

    public void desativar(Long id) {
        Usuario usuario = buscarPorId(id);

        usuario.setAtivo(false);

        usuarioRepository.save(usuario);
    }

    private void validarCadastro(Usuario dados) {
        validarDadosCliente(dados);

        String senha = dados.getSenha();
        Perfil perfil = dados.getPerfil();

        if (senha == null || senha.isBlank()) {
            throw new IllegalArgumentException("Senha obrigatória");
        }
        if (perfil == null) {
            throw new IllegalArgumentException("Perfil obrigatório");
        }
    }

    private void validarDadosCliente(Usuario dados) {
        if (dados == null) {
            throw new IllegalArgumentException("Dados do usuário obrigatórios");
        }

        String nome = dados.getNome();
        String email = dados.getEmail();

        if (nome == null || nome.isBlank() || nome.strip().length() > 120) {
            throw new IllegalArgumentException("Nome inválido");
        }
        if (email == null || email.length() > 150 || !EMAIL_VALIDO.matcher(email).matches()) {
            throw new IllegalArgumentException("E-mail inválido");
        }
    }

    private boolean validarEmail(String email, Long id) {
        Optional<Usuario> encontrado = usuarioRepository.findByEmail(email);
        Usuario usuarioEncontrado = encontrado.orElse(null);
        
        if (usuarioEncontrado == null) {
            return false;
        }

        Long idEncontrado = usuarioEncontrado.getId();
        return id == null || !Objects.equals(idEncontrado, id);
    }

    private String normalizarEmail(String email) {
        return email.toLowerCase(Locale.ROOT);
    }
}
