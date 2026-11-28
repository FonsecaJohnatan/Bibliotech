package br.edu.bibliotech.service;

import jakarta.persistence.EntityNotFoundException;

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
import static org.assertj.core.api.Assertions.catchThrowable;
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
        Autor novo = new Autor("Machado de Assis", "Brasileira", LocalDate.of(1839, 6, 21));
        when(autorRepository.save(any(Autor.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        Autor salvo = autorService.cadastrar(novo);

        assertThat(salvo.getNome()).isEqualTo("Machado de Assis");
        assertThat(salvo.getNacionalidade()).isEqualTo("Brasileira");
        assertThat(salvo.getDataNascimento()).isEqualTo(LocalDate.of(1839, 6, 21));
        verify(autorRepository).save(any(Autor.class));
    }

    @Test
    void deveCadastrarSemDadosOpcionais() {
        Autor novo = new Autor("Machado de Assis", null, null);
        when(autorRepository.save(any(Autor.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        Autor salvo = autorService.cadastrar(novo);

        assertThat(salvo.getNacionalidade()).isNull();
        assertThat(salvo.getDataNascimento()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void naoDeveCadastrarNomeVazio(String nome) {
        Autor novo = new Autor(nome, "Brasileira", null);

        Throwable erro = catchThrowable(() -> autorService.cadastrar(novo));

        assertThat(erro).isInstanceOf(IllegalArgumentException.class);

        verify(autorRepository, never()).save(any(Autor.class));
    }

    @Test
    void naoDeveCadastrarNomeAcimaDoLimite() {
        Autor novo = new Autor("A".repeat(121), "Brasileira", null);

        Throwable erro = catchThrowable(() -> autorService.cadastrar(novo));

        assertThat(erro).isInstanceOf(IllegalArgumentException.class);

        verify(autorRepository, never()).save(any(Autor.class));
    }

    @Test
    void naoDeveCadastrarNacionalidadeAcimaDoLimite() {
        Autor novo = new Autor("Machado de Assis", "B".repeat(61), null);

        Throwable erro = catchThrowable(() -> autorService.cadastrar(novo));

        assertThat(erro).isInstanceOf(IllegalArgumentException.class);

        verify(autorRepository, never()).save(any(Autor.class));
    }

    @Test
    void deveBuscarAutorPorId() {
        Autor existente = new Autor("Machado de Assis", "Brasileira", null);
        existente.setId(10L);
        when(autorRepository.findById(10L)).thenReturn(Optional.of(existente));

        var resultado = autorService.buscarPorId(10L);

        assertThat(resultado).isSameAs(existente);
    }

    @Test
    void naoDeveBuscarAutorInexistente() {
        when(autorRepository.findById(999L)).thenReturn(Optional.empty());

        Throwable erro = catchThrowable(() -> autorService.buscarPorId(999L));

        assertThat(erro).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deveListarAutores() {
        Autor primeiro = new Autor("Machado de Assis", "Brasileira", null);
        Autor segundo = new Autor("Clarice Lispector", "Brasileira", null);
        when(autorRepository.findAll()).thenReturn(List.of(primeiro, segundo));

        var resultado = autorService.listar();

        assertThat(resultado).containsExactly(primeiro, segundo);
    }

    @Test
    void deveAtualizarAutorSemAlterarLivros() {
        Autor existente = new Autor("Machado de Assis", "Brasileira", null);
        existente.setId(10L);
        Livro livro = new Livro();
        existente.getLivros().add(livro);
        LocalDate novaData = LocalDate.of(1839, 6, 21);
        Autor novosDados = new Autor("Joaquim Maria Machado de Assis", "Brasil", novaData);
        when(autorRepository.findById(10L)).thenReturn(Optional.of(existente));
        when(autorRepository.save(any(Autor.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        Autor atualizado = autorService.atualizar(10L, novosDados);

        assertThat(atualizado.getId()).isEqualTo(10L);
        assertThat(atualizado.getNome()).isEqualTo("Joaquim Maria Machado de Assis");
        assertThat(atualizado.getNacionalidade()).isEqualTo("Brasil");
        assertThat(atualizado.getDataNascimento()).isEqualTo(novaData);
        assertThat(atualizado.getLivros()).containsExactly(livro);
    }

    @Test
    void naoDeveAtualizarAutorInexistente() {
        when(autorRepository.findById(999L)).thenReturn(Optional.empty());

        Throwable erro = catchThrowable(() -> autorService.atualizar(999L, new Autor("Nome", null, null)));

        assertThat(erro).isInstanceOf(EntityNotFoundException.class);

        verify(autorRepository, never()).save(any(Autor.class));
    }

    @Test
    void naoDeveAtualizarComNomeVazio() {
        Autor existente = new Autor("Machado de Assis", "Brasileira", null);
        existente.setId(10L);
        when(autorRepository.findById(10L)).thenReturn(Optional.of(existente));

        Throwable erro = catchThrowable(() -> autorService.atualizar(10L, new Autor("  ", "Brasil", null)));

        assertThat(erro).isInstanceOf(IllegalArgumentException.class);

        verify(autorRepository, never()).save(any(Autor.class));
        assertThat(existente.getNome()).isEqualTo("Machado de Assis");
    }

    @Test
    void deveExcluirAutorExistente() {
        Autor existente = new Autor("Machado de Assis", "Brasileira", null);
        existente.setId(10L);
        when(autorRepository.findById(10L)).thenReturn(Optional.of(existente));

        autorService.excluir(10L);

        verify(autorRepository).delete(existente);
    }

    @Test
    void naoDeveExcluirAutorInexistente() {
        when(autorRepository.findById(999L)).thenReturn(Optional.empty());

        Throwable erro = catchThrowable(() -> autorService.excluir(999L));

        assertThat(erro).isInstanceOf(EntityNotFoundException.class);

        verify(autorRepository, never()).delete(any(Autor.class));
    }

}
