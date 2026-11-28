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

    private final Pattern padraoEmail = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
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

        validarEmailDisponivel(email, null);

        String senhaCriptografada = passwordEncoder.encode(senha);
        Usuario usuario = new Usuario(nome, email, senhaCriptografada, perfil);

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

        validarEmailDisponivel(email, id);

        usuario.setNome(nome);
        usuario.setEmail(email);

        return usuarioRepository.save(usuario);
    }

    public Usuario alterarSenha(Long id, String senhaAtual, String novaSenha) {
        Usuario usuario = buscarPorId(id);
        String hashAtual = usuario.getSenha();

        validarSenhaAtual(senhaAtual, hashAtual);
        validarNovaSenha(novaSenha);

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

        validarNome(dados.getNome());
        validarFormatoEmail(dados.getEmail());
    }

    private boolean emailPertenceAOutroUsuario(String email, Long id) {
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

    private void validarNome(String nome) {
        boolean nomeInvalido = nome == null || nome.isBlank() || nome.strip().length() > 120;

        if (nomeInvalido) {
            throw new IllegalArgumentException("Nome inválido");
        }
    }

    private void validarFormatoEmail(String email) {
        boolean emailInvalido = email == null || email.length() > 150
                || !padraoEmail.matcher(email).matches();

        if (emailInvalido) {
            throw new IllegalArgumentException("E-mail inválido");
        }
    }

    private void validarEmailDisponivel(String email, Long usuarioId) {
        if (emailPertenceAOutroUsuario(email, usuarioId)) {
            throw new IllegalArgumentException("E-mail já cadastrado");
        }
    }

    private void validarSenhaAtual(String senhaAtual, String hashAtual) {
        boolean senhaIncorreta = senhaAtual == null || !passwordEncoder.matches(senhaAtual, hashAtual);

        if (senhaIncorreta) {
            throw new IllegalArgumentException("Senha atual incorreta");
        }
    }

    private void validarNovaSenha(String novaSenha) {
        boolean senhaVazia = novaSenha == null || novaSenha.isBlank();

        if (senhaVazia) {
            throw new IllegalArgumentException("Nova senha obrigatória");
        }
    }
}
