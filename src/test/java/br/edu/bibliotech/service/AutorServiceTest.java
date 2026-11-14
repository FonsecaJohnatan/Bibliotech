package br.edu.bibliotech.service;

import br.edu.bibliotech.model.Autor;
import br.edu.bibliotech.model.Livro;
import br.edu.bibliotech.repository.AutorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutorServiceTest {

    @Mock
    private AutorRepository autorRepository;

    @InjectMocks
    private AutorService autorService;

    @Test
    void deveCadastrarAutorValido() {
        Autor novo = autor("Machado de Assis", "Brasileira", LocalDate.of(1839, 6, 21));
        when(autorRepository.save(any(Autor.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Autor salvo = autorService.cadastrar(novo);

        assertThat(salvo.getNome()).isEqualTo("Machado de Assis");
        assertThat(salvo.getNacionalidade()).isEqualTo("Brasileira");
        assertThat(salvo.getDataNascimento()).isEqualTo(LocalDate.of(1839, 6, 21));
        verify(autorRepository).save(any(Autor.class));
    }

    @Test
    void deveCadastrarSemDadosOpcionais() {
        Autor novo = autor("Machado de Assis", null, null);
        when(autorRepository.save(any(Autor.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Autor salvo = autorService.cadastrar(novo);

        assertThat(salvo.getNacionalidade()).isNull();
        assertThat(salvo.getDataNascimento()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void naoDeveCadastrarNomeVazio(String nome) {
        Autor novo = autor(nome, "Brasileira", null);

        assertThatThrownBy(() -> autorService.cadastrar(novo)).isInstanceOf(RuntimeException.class);

        verify(autorRepository, never()).save(any(Autor.class));
    }

    @Test
    void naoDeveCadastrarNomeAcimaDoLimite() {
        Autor novo = autor("A".repeat(121), "Brasileira", null);

        assertThatThrownBy(() -> autorService.cadastrar(novo)).isInstanceOf(RuntimeException.class);

        verify(autorRepository, never()).save(any(Autor.class));
    }

    @Test
    void naoDeveCadastrarNacionalidadeAcimaDoLimite() {
        Autor novo = autor("Machado de Assis", "B".repeat(61), null);

        assertThatThrownBy(() -> autorService.cadastrar(novo)).isInstanceOf(RuntimeException.class);

        verify(autorRepository, never()).save(any(Autor.class));
    }

    @Test
    void deveBuscarAutorPorId() {
        Autor existente = autor("Machado de Assis", "Brasileira", null);
        existente.setId(1L);
        when(autorRepository.findById(1L)).thenReturn(Optional.of(existente));

        assertThat(autorService.buscarPorId(1L)).isSameAs(existente);
    }

    @Test
    void naoDeveBuscarAutorInexistente() {
        when(autorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> autorService.buscarPorId(99L)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void deveListarAutores() {
        Autor primeiro = autor("Machado de Assis", "Brasileira", null);
        Autor segundo = autor("Clarice Lispector", "Brasileira", null);
        when(autorRepository.findAll()).thenReturn(List.of(primeiro, segundo));

        assertThat(autorService.listar()).containsExactly(primeiro, segundo);
    }

    @Test
    void deveAtualizarAutorSemAlterarLivros() {
        Autor existente = autor("Machado de Assis", "Brasileira", null);
        existente.setId(1L);
        Livro livro = new Livro();
        existente.getLivros().add(livro);
        LocalDate novaData = LocalDate.of(1839, 6, 21);
        Autor novosDados = autor("Joaquim Maria Machado de Assis", "Brasil", novaData);
        when(autorRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(autorRepository.save(any(Autor.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Autor atualizado = autorService.atualizar(1L, novosDados);

        assertThat(atualizado.getId()).isEqualTo(1L);
        assertThat(atualizado.getNome()).isEqualTo("Joaquim Maria Machado de Assis");
        assertThat(atualizado.getNacionalidade()).isEqualTo("Brasil");
        assertThat(atualizado.getDataNascimento()).isEqualTo(novaData);
        assertThat(atualizado.getLivros()).containsExactly(livro);
    }

    @Test
    void naoDeveAtualizarAutorInexistente() {
        when(autorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> autorService.atualizar(99L, autor("Nome", null, null)))
                .isInstanceOf(RuntimeException.class);

        verify(autorRepository, never()).save(any(Autor.class));
    }

    @Test
    void naoDeveAtualizarComNomeVazio() {
        Autor existente = autor("Machado de Assis", "Brasileira", null);
        existente.setId(1L);
        when(autorRepository.findById(1L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> autorService.atualizar(1L, autor("  ", "Brasil", null)))
                .isInstanceOf(RuntimeException.class);

        verify(autorRepository, never()).save(any(Autor.class));
        assertThat(existente.getNome()).isEqualTo("Machado de Assis");
    }

    @Test
    void deveExcluirAutorExistente() {
        Autor existente = autor("Machado de Assis", "Brasileira", null);
        existente.setId(1L);
        when(autorRepository.findById(1L)).thenReturn(Optional.of(existente));

        autorService.excluir(1L);

        verify(autorRepository).delete(existente);
    }

    @Test
    void naoDeveExcluirAutorInexistente() {
        when(autorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> autorService.excluir(99L)).isInstanceOf(RuntimeException.class);

        verify(autorRepository, never()).delete(any(Autor.class));
    }

    private Autor autor(String nome, String nacionalidade, LocalDate dataNascimento) {
        return new Autor(nome, nacionalidade, dataNascimento);
    }
}
