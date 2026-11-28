package br.edu.bibliotech.service;

import jakarta.persistence.EntityNotFoundException;

import br.edu.bibliotech.model.Autor;
import br.edu.bibliotech.model.Categoria;
import br.edu.bibliotech.model.Exemplar;
import br.edu.bibliotech.model.Livro;
import br.edu.bibliotech.repository.AutorRepository;
import br.edu.bibliotech.repository.CategoriaRepository;
import br.edu.bibliotech.repository.LivroRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LivroServiceTest {

    @Mock
    private LivroRepository livroRepository;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private AutorRepository autorRepository;
    @InjectMocks
    private LivroService livroService;

    @Test
    void deveCadastrarLivroComCategoriaEAutoresExistentes() {
        Categoria categoriaInformada = new Categoria("Romance", null);
        categoriaInformada.setId(10L);
        Categoria categoriaSalva = new Categoria("Romance", "Ficção");
        categoriaSalva.setId(10L);
        Autor primeiroInformado = new Autor("Machado", null, null);
        primeiroInformado.setId(20L);
        Autor segundoInformado = new Autor("Coautor", null, null);
        segundoInformado.setId(21L);
        Autor primeiroSalvo = new Autor("Machado de Assis", "Brasileira", null);
        primeiroSalvo.setId(20L);
        Autor segundoSalvo = new Autor("Outro autor", "Brasileira", null);
        segundoSalvo.setId(21L);
        Livro dados = new Livro("  Dom Casmurro  ", "9780110002224", 1899, categoriaInformada);
        dados.getAutores().add(primeiroInformado);
        dados.getAutores().add(segundoInformado);

        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoriaSalva));
        when(autorRepository.findById(20L)).thenReturn(Optional.of(primeiroSalvo));
        when(autorRepository.findById(21L)).thenReturn(Optional.of(segundoSalvo));
        when(livroRepository.save(any(Livro.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        Livro salvo = livroService.cadastrar(dados);

        assertThat(salvo.getTitulo()).isEqualTo("Dom Casmurro");
        assertThat(salvo.getIsbn()).isEqualTo("9780110002224");
        assertThat(salvo.getAnoPublicacao()).isEqualTo(1899);
        assertThat(salvo.getCategoria()).isSameAs(categoriaSalva);
        assertThat(salvo.getAutores()).containsExactlyInAnyOrder(primeiroSalvo, segundoSalvo);

        verify(livroRepository).save(any(Livro.class));
    }

    @Test
    void deveCadastrarLivroSemAutores() {
        Categoria categoria = new Categoria("Romance", null);
        categoria.setId(10L);

        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria));
        when(livroRepository.save(any(Livro.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        Livro salvo = livroService.cadastrar(new Livro("Livro anônimo", "9780110002224", null, categoria));

        assertThat(salvo.getAutores()).isEmpty();
        assertThat(salvo.getCategoria()).isSameAs(categoria);
    }

    @Test
    void deveNormalizarIsbnComSeparadores() {
        Categoria categoria = new Categoria("Romance", null);
        categoria.setId(10L);

        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria));
        when(livroRepository.save(any(Livro.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        Livro salvo = livroService.cadastrar(new Livro("Livro", "978-0-11-000222-4", null, categoria));

        assertThat(salvo.getIsbn()).isEqualTo("9780110002224");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void naoDeveCadastrarTituloVazio(String titulo) {
        Livro dados = new Livro(titulo, "9780110002224", null, new Categoria("Romance", null));

        Throwable erro = catchThrowable(() -> livroService.cadastrar(dados));

        assertThat(erro).isInstanceOf(IllegalArgumentException.class);

        verify(livroRepository, never()).save(any(Livro.class));
    }

    @Test
    void naoDeveCadastrarTituloAcimaDoLimite() {
        Livro dados = new Livro("T".repeat(201), "9780110002224", null, null);

        Throwable erro = catchThrowable(() -> livroService.cadastrar(dados));

        assertThat(erro).isInstanceOf(IllegalArgumentException.class);

        verify(livroRepository, never()).save(any(Livro.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"123", "978011000222X", "9780110002225", "9770110002227"})
    void naoDeveCadastrarIsbnInvalido(String isbn) {
        Livro dados = new Livro("Livro", isbn, null, null);

        Throwable erro = catchThrowable(() -> livroService.cadastrar(dados));

        assertThat(erro).isInstanceOf(IllegalArgumentException.class);

        verify(livroRepository, never()).save(any(Livro.class));
    }

    @Test
    void naoDeveCadastrarIsbnDuplicado() {
        Livro existente = new Livro("Outro livro", "9780110002224", null, null);
        existente.setId(30L);
        when(livroRepository.findByIsbn("9780110002224")).thenReturn(Optional.of(existente));

        Throwable erro = catchThrowable(() -> livroService.cadastrar(
                new Livro("Livro", "9780110002224", null, null)));

        assertThat(erro).isInstanceOf(IllegalArgumentException.class);

        verify(livroRepository, never()).save(any(Livro.class));
    }

    @Test
    void naoDeveCadastrarSemCategoria() {
        Livro dados = new Livro("Livro", "9780110002224", null, null);

        Throwable erro = catchThrowable(() -> livroService.cadastrar(dados));

        assertThat(erro).isInstanceOf(IllegalArgumentException.class);

        verify(livroRepository, never()).save(any(Livro.class));
    }

    @Test
    void naoDeveCadastrarCategoriaSemId() {
        Livro dados = new Livro("Livro", "9780110002224", null,
                new Categoria("Romance", null));

        Throwable erro = catchThrowable(() -> livroService.cadastrar(dados));

        assertThat(erro).isInstanceOf(IllegalArgumentException.class);

        verify(livroRepository, never()).save(any(Livro.class));
    }

    @Test
    void naoDeveCadastrarCategoriaInexistente() {
        Categoria categoria = new Categoria("Romance", null);
        categoria.setId(999L);
        when(categoriaRepository.findById(999L)).thenReturn(Optional.empty());

        Throwable erro = catchThrowable(() -> livroService.cadastrar(
                new Livro("Livro", "9780110002224", null, categoria)));

        assertThat(erro).isInstanceOf(EntityNotFoundException.class);

        verify(livroRepository, never()).save(any(Livro.class));
    }

    @Test
    void naoDeveCadastrarAutorInexistente() {
        Categoria categoria = new Categoria("Romance", null);
        categoria.setId(10L);
        Autor autor = new Autor("Desconhecido", null, null);
        autor.setId(999L);
        Livro dados = new Livro("Livro", "9780110002224", null, categoria);
        dados.getAutores().add(autor);

        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria));
        when(autorRepository.findById(999L)).thenReturn(Optional.empty());

        Throwable erro = catchThrowable(() -> livroService.cadastrar(dados));

        assertThat(erro).isInstanceOf(EntityNotFoundException.class);

        verify(livroRepository, never()).save(any(Livro.class));
    }

    @Test
    void naoDeveCadastrarAutorSemId() {
        Categoria categoria = new Categoria("Romance", null);
        categoria.setId(10L);
        Livro dados = new Livro("Livro", "9780110002224", null, categoria);
        dados.getAutores().add(new Autor("Autor não salvo", null, null));
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria));

        Throwable erro = catchThrowable(() -> livroService.cadastrar(dados));

        assertThat(erro).isInstanceOf(IllegalArgumentException.class);

        verify(livroRepository, never()).save(any(Livro.class));
    }

    @Test
    void deveBuscarLivroPorId() {
        Livro existente = new Livro("Livro", "9780110002224", null, null);
        existente.setId(30L);
        when(livroRepository.findById(30L)).thenReturn(Optional.of(existente));

        var resultado = livroService.buscarPorId(30L);

        assertThat(resultado).isSameAs(existente);
    }

    @Test
    void naoDeveBuscarLivroInexistente() {
        when(livroRepository.findById(999L)).thenReturn(Optional.empty());

        Throwable erro = catchThrowable(() -> livroService.buscarPorId(999L));

        assertThat(erro).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deveListarLivros() {
        Livro primeiro = new Livro("Primeiro", "9780110002224", null, null);
        Livro segundo = new Livro("Segundo", "9780110002231", null, null);
        when(livroRepository.findAll()).thenReturn(List.of(primeiro, segundo));

        var resultado = livroService.listar();

        assertThat(resultado).containsExactly(primeiro, segundo);
    }

    @Test
    void deveAtualizarLivroSemAlterarExemplares() {
        Categoria categoriaAntiga = new Categoria("Romance", null);
        categoriaAntiga.setId(10L);
        Categoria categoriaNova = new Categoria("Ciência", null);
        categoriaNova.setId(11L);
        Autor autorNovo = new Autor("Novo autor", null, null);
        autorNovo.setId(21L);
        Livro existente = new Livro("Antigo", "9780110002224", 2000, categoriaAntiga);
        existente.setId(30L);
        Exemplar exemplar = new Exemplar();
        existente.getExemplares().add(exemplar);
        Livro dados = new Livro("  Novo título  ", "9780110002231", 2020, categoriaNova);
        dados.getAutores().add(autorNovo);

        when(livroRepository.findById(30L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.findById(11L)).thenReturn(Optional.of(categoriaNova));
        when(autorRepository.findById(21L)).thenReturn(Optional.of(autorNovo));
        when(livroRepository.save(any(Livro.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        Livro atualizado = livroService.atualizar(30L, dados);

        assertThat(atualizado.getId()).isEqualTo(30L);
        assertThat(atualizado.getTitulo()).isEqualTo("Novo título");
        assertThat(atualizado.getIsbn()).isEqualTo("9780110002231");
        assertThat(atualizado.getAnoPublicacao()).isEqualTo(2020);
        assertThat(atualizado.getCategoria()).isSameAs(categoriaNova);
        assertThat(atualizado.getAutores()).containsExactly(autorNovo);
        assertThat(atualizado.getExemplares()).containsExactly(exemplar);
    }

    @Test
    void devePermitirAtualizarMantendoOProprioIsbn() {
        Categoria categoria = new Categoria("Romance", null);
        categoria.setId(10L);
        Livro existente = new Livro("Antigo", "9780110002224", null, categoria);
        existente.setId(30L);

        when(livroRepository.findById(30L)).thenReturn(Optional.of(existente));
        when(livroRepository.findByIsbn("9780110002224")).thenReturn(Optional.of(existente));
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria));
        when(livroRepository.save(any(Livro.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        Livro atualizado = livroService.atualizar(30L,
                new Livro("Novo título", "9780110002224", null, categoria));

        assertThat(atualizado.getTitulo()).isEqualTo("Novo título");
        assertThat(atualizado.getIsbn()).isEqualTo("9780110002224");
    }

    @Test
    void naoDeveAtualizarComIsbnDeOutroLivro() {
        Livro existente = new Livro("Antigo", "9780110002224", null, null);
        existente.setId(30L);
        Livro outro = new Livro("Outro", "9780110002231", null, null);
        outro.setId(31L);

        when(livroRepository.findById(30L)).thenReturn(Optional.of(existente));
        when(livroRepository.findByIsbn("9780110002231")).thenReturn(Optional.of(outro));

        Throwable erro = catchThrowable(() -> livroService.atualizar(30L,
                new Livro("Novo", "9780110002231", null, null)));

        assertThat(erro).isInstanceOf(IllegalArgumentException.class);

        assertThat(existente.getTitulo()).isEqualTo("Antigo");
        verify(livroRepository, never()).save(any(Livro.class));
    }

    @Test
    void naoDeveAtualizarComCategoriaInexistente() {
        Livro existente = new Livro("Antigo", "9780110002224", null, null);
        existente.setId(30L);
        Categoria categoria = new Categoria("Desconhecida", null);
        categoria.setId(999L);

        when(livroRepository.findById(30L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.findById(999L)).thenReturn(Optional.empty());

        Throwable erro = catchThrowable(() -> livroService.atualizar(30L,
                new Livro("Novo", "9780110002231", null, categoria)));

        assertThat(erro).isInstanceOf(EntityNotFoundException.class);

        assertThat(existente.getTitulo()).isEqualTo("Antigo");
        verify(livroRepository, never()).save(any(Livro.class));
    }

    @Test
    void naoDeveAtualizarComAutorInexistente() {
        Livro existente = new Livro("Antigo", "9780110002224", null, null);
        existente.setId(30L);
        Categoria categoria = new Categoria("Romance", null);
        categoria.setId(10L);
        Autor autor = new Autor("Desconhecido", null, null);
        autor.setId(999L);
        Livro dados = new Livro("Novo", "9780110002231", null, categoria);
        dados.getAutores().add(autor);

        when(livroRepository.findById(30L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria));
        when(autorRepository.findById(999L)).thenReturn(Optional.empty());

        Throwable erro = catchThrowable(() -> livroService.atualizar(30L, dados));

        assertThat(erro).isInstanceOf(EntityNotFoundException.class);

        assertThat(existente.getTitulo()).isEqualTo("Antigo");
        verify(livroRepository, never()).save(any(Livro.class));
    }

    @Test
    void naoDeveAtualizarLivroInexistente() {
        when(livroRepository.findById(999L)).thenReturn(Optional.empty());

        Throwable erro = catchThrowable(() -> livroService.atualizar(999L,
                new Livro("Novo", "9780110002224", null, null)));

        assertThat(erro).isInstanceOf(EntityNotFoundException.class);

        verify(livroRepository, never()).save(any(Livro.class));
    }

    @Test
    void deveExcluirLivroExistente() {
        Livro existente = new Livro("Livro", "9780110002224", null, null);
        existente.setId(30L);
        when(livroRepository.findById(30L)).thenReturn(Optional.of(existente));

        livroService.excluir(30L);

        verify(livroRepository).delete(existente);
    }

    @Test
    void naoDeveExcluirLivroInexistente() {
        when(livroRepository.findById(999L)).thenReturn(Optional.empty());

        Throwable erro = catchThrowable(() -> livroService.excluir(999L));

        assertThat(erro).isInstanceOf(EntityNotFoundException.class);

        verify(livroRepository, never()).delete(any(Livro.class));
    }

}
