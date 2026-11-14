package br.edu.bibliotech.service;

import br.edu.bibliotech.model.Perfil;
import br.edu.bibliotech.model.Emprestimo;
import br.edu.bibliotech.model.Usuario;
import br.edu.bibliotech.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void deveCadastrarUsuarioValido() {
        Usuario novo = usuario("Maria", "MARIA@EMAIL.COM");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Usuario salvo = usuarioService.cadastrar(novo);

        assertThat(salvo.getNome()).isEqualTo("Maria");
        assertThat(salvo.getEmail()).isEqualTo("maria@email.com");
        assertThat(salvo.getPerfil()).isEqualTo(Perfil.LEITOR);
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void naoDeveCadastrarNomeVazio(String nome) {
        Usuario novo = usuario(nome, "maria@email.com");

        assertThatThrownBy(() -> usuarioService.cadastrar(novo)).isInstanceOf(RuntimeException.class);

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "sem-arroba", "@email.com", "maria@"})
    void naoDeveCadastrarEmailInvalido(String email) {
        Usuario novo = usuario("Maria", email);

        assertThatThrownBy(() -> usuarioService.cadastrar(novo)).isInstanceOf(RuntimeException.class);

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void naoDeveCadastrarEmailDuplicado() {
        Usuario existente = usuario("Maria", "maria@email.com");
        when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> usuarioService.cadastrar(usuario("Outra", "MARIA@EMAIL.COM")))
                .isInstanceOf(RuntimeException.class);

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @ParameterizedTest
    @EnumSource(Perfil.class)
    void deveAceitarPerfisValidos(Perfil perfil) {
        Usuario novo = usuario("Maria", "maria@email.com");
        novo.setPerfil(perfil);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        assertThat(usuarioService.cadastrar(novo).getPerfil()).isEqualTo(perfil);
    }

    @Test
    void naoDeveCadastrarSemPerfil() {
        Usuario novo = usuario("Maria", "maria@email.com");
        novo.setPerfil(null);

        assertThatThrownBy(() -> usuarioService.cadastrar(novo)).isInstanceOf(RuntimeException.class);

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void deveBuscarUsuarioPorId() {
        Usuario existente = usuario("Maria", "maria@email.com");
        existente.setId(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));

        assertThat(usuarioService.buscarPorId(1L)).isSameAs(existente);
    }

    @Test
    void naoDeveBuscarUsuarioInexistente() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.buscarPorId(99L)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void deveListarUsuarios() {
        Usuario primeiro = usuario("Maria", "maria@email.com");
        Usuario segundo = usuario("João", "joao@email.com");
        when(usuarioRepository.findAll()).thenReturn(List.of(primeiro, segundo));

        assertThat(usuarioService.listar()).containsExactly(primeiro, segundo);
    }

    @Test
    void deveAtualizarUsuario() {
        Usuario existente = usuario("Maria", "maria@email.com");
        existente.setId(1L);
        String senhaAnterior = existente.getSenha();
        Usuario novosDados = usuario("Maria Silva", "MARIA.SILVA@EMAIL.COM");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Usuario atualizado = usuarioService.atualizar(1L, novosDados);

        assertThat(atualizado.getNome()).isEqualTo("Maria Silva");
        assertThat(atualizado.getEmail()).isEqualTo("maria.silva@email.com");
        assertThat(atualizado.getSenha()).isEqualTo(senhaAnterior);
        assertThat(atualizado.getPerfil()).isEqualTo(Perfil.LEITOR);
    }

    @Test
    void naoDeveAtualizarComEmailDeOutroUsuario() {
        Usuario existente = usuario("Maria", "maria@email.com");
        existente.setId(1L);
        Usuario outro = usuario("João", "joao@email.com");
        outro.setId(2L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(outro));

        assertThatThrownBy(() -> usuarioService.atualizar(1L, usuario("Maria", "JOAO@EMAIL.COM")))
                .isInstanceOf(RuntimeException.class);

        verify(usuarioRepository, never()).save(any(Usuario.class));
        assertThat(existente.getEmail()).isEqualTo("maria@email.com");
    }

    @Test
    void naoDeveAtualizarUsuarioInexistente() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.atualizar(99L, usuario("Maria", "maria@email.com")))
                .isInstanceOf(RuntimeException.class);

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void naoDeveAtualizarComNomeVazio() {
        Usuario existente = usuario("Maria", "maria@email.com");
        existente.setId(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> usuarioService.atualizar(1L, usuario("  ", "novo@email.com")))
                .isInstanceOf(RuntimeException.class);

        verify(usuarioRepository, never()).save(any(Usuario.class));
        assertThat(existente.getNome()).isEqualTo("Maria");
    }

    @Test
    void naoDeveAtualizarComEmailInvalido() {
        Usuario existente = usuario("Maria", "maria@email.com");
        existente.setId(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> usuarioService.atualizar(1L, usuario("Novo nome", "email-invalido")))
                .isInstanceOf(RuntimeException.class);

        verify(usuarioRepository, never()).save(any(Usuario.class));
        assertThat(existente.getNome()).isEqualTo("Maria");
        assertThat(existente.getEmail()).isEqualTo("maria@email.com");
    }

    @Test
    void deveDesativarUsuario() {
        Usuario existente = usuario("Maria", "maria@email.com");
        existente.setId(1L);
        Emprestimo emprestimo = new Emprestimo();
        existente.getEmprestimos().add(emprestimo);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));

        usuarioService.desativar(1L);

        assertThat(existente.getAtivo()).isFalse();
        assertThat(existente.getEmprestimos()).containsExactly(emprestimo);
        verify(usuarioRepository, never()).delete(any(Usuario.class));
    }

    @Test
    void naoDeveDesativarUsuarioInexistente() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.desativar(99L)).isInstanceOf(RuntimeException.class);

        verify(usuarioRepository, never()).delete(any(Usuario.class));
    }

    @Test
    void deveProtegerSenha() {
        Usuario novo = usuario("Maria", "maria@email.com");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Usuario salvo = usuarioService.cadastrar(novo);

        assertThat(salvo.getSenha()).isNotEqualTo("senhaEmTextoPuro");
        assertThat(salvo.getSenha()).matches("^\\$2[aby]\\$\\d{2}\\$.{53}$");
    }

    @Test
    void deveAlterarSenhaComSenhaAtualCorreta() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        Usuario existente = usuario("Maria", "maria@email.com");
        existente.setId(1L);
        existente.setSenha(encoder.encode("senhaAntiga"));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Usuario atualizado = usuarioService.alterarSenha(1L, "senhaAntiga", "senhaNova");

        assertThat(encoder.matches("senhaNova", atualizado.getSenha())).isTrue();
        assertThat(encoder.matches("senhaAntiga", atualizado.getSenha())).isFalse();
        assertThat(atualizado.getNome()).isEqualTo("Maria");
        assertThat(atualizado.getEmail()).isEqualTo("maria@email.com");
        verify(usuarioRepository).save(existente);
    }

    @Test
    void naoDeveAlterarSenhaComSenhaAtualIncorreta() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        Usuario existente = usuario("Maria", "maria@email.com");
        existente.setId(1L);
        String hashOriginal = encoder.encode("senhaAntiga");
        existente.setSenha(hashOriginal);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> usuarioService.alterarSenha(1L, "senhaErrada", "senhaNova"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(existente.getSenha()).isEqualTo(hashOriginal);
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void naoDeveAlterarParaSenhaVazia(String novaSenha) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        Usuario existente = usuario("Maria", "maria@email.com");
        existente.setId(1L);
        String hashOriginal = encoder.encode("senhaAntiga");
        existente.setSenha(hashOriginal);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> usuarioService.alterarSenha(1L, "senhaAntiga", novaSenha))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(existente.getSenha()).isEqualTo(hashOriginal);
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void naoDeveAlterarSenhaDeUsuarioInexistente() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.alterarSenha(99L, "senhaAntiga", "senhaNova"))
                .isInstanceOf(RuntimeException.class);

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    private Usuario usuario(String nome, String email) {
        return new Usuario(nome, email, "senhaEmTextoPuro", Perfil.LEITOR);
    }
}
