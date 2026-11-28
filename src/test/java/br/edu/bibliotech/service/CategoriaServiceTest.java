package br.edu.bibliotech.service;

import jakarta.persistence.EntityNotFoundException;

import br.edu.bibliotech.model.Categoria;
import br.edu.bibliotech.model.Livro;
import br.edu.bibliotech.repository.CategoriaRepository;
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
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    @Test
    void deveCadastrarCategoriaValida() {
        Categoria nova = new Categoria("  Romance  ", "Livros de ficção");
        when(categoriaRepository.save(any(Categoria.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        Categoria salva = categoriaService.cadastrar(nova);

        assertThat(salva.getNome()).isEqualTo("Romance");
        assertThat(salva.getDescricao()).isEqualTo("Livros de ficção");
        verify(categoriaRepository).save(any(Categoria.class));
    }

    @Test
    void deveCadastrarSemDescricao() {
        when(categoriaRepository.save(any(Categoria.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        Categoria salva = categoriaService.cadastrar(new Categoria("Romance", null));

        assertThat(salva.getDescricao()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void naoDeveCadastrarNomeVazio(String nome) {
        Throwable erro = catchThrowable(() -> categoriaService.cadastrar(new Categoria(nome, null)));

        assertThat(erro).isInstanceOf(IllegalArgumentException.class);

        verify(categoriaRepository, never()).save(any(Categoria.class));
    }

    @Test
    void naoDeveCadastrarNomeAcimaDoLimite() {
        Categoria nova = new Categoria("A".repeat(81), null);

        Throwable erro = catchThrowable(() -> categoriaService.cadastrar(nova));

        assertThat(erro).isInstanceOf(IllegalArgumentException.class);

        verify(categoriaRepository, never()).save(any(Categoria.class));
    }

    @Test
    void naoDeveCadastrarDescricaoAcimaDoLimite() {
        Categoria nova = new Categoria("Romance", "D".repeat(256));

        Throwable erro = catchThrowable(() -> categoriaService.cadastrar(nova));

        assertThat(erro).isInstanceOf(IllegalArgumentException.class);

        verify(categoriaRepository, never()).save(any(Categoria.class));
    }

    @Test
    void naoDeveCadastrarNomeDuplicadoIgnorandoMaiusculas() {
        when(categoriaRepository.existsByNomeIgnoreCase("ROMANCE")).thenReturn(true);

        Throwable erro = catchThrowable(() -> categoriaService.cadastrar(new Categoria("ROMANCE", null)));

        assertThat(erro).isInstanceOf(IllegalArgumentException.class);

        verify(categoriaRepository, never()).save(any(Categoria.class));
    }

    @Test
    void deveBuscarCategoriaPorId() {
        Categoria existente = new Categoria("Romance", null);
        existente.setId(10L);
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(existente));

        var resultado = categoriaService.buscarPorId(10L);

        assertThat(resultado).isSameAs(existente);
    }

    @Test
    void naoDeveBuscarCategoriaInexistente() {
        when(categoriaRepository.findById(999L)).thenReturn(Optional.empty());

        Throwable erro = catchThrowable(() -> categoriaService.buscarPorId(999L));

        assertThat(erro).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deveListarCategorias() {
        Categoria romance = new Categoria("Romance", null);
        romance.setId(10L);
        Categoria ciencia = new Categoria("Ciência", null);
        ciencia.setId(20L);
        when(categoriaRepository.findAll()).thenReturn(List.of(romance, ciencia));

        var resultado = categoriaService.listar();

        assertThat(resultado).containsExactly(romance, ciencia);
    }

    @Test
    void deveAtualizarCategoriaSemAlterarLivros() {
        Categoria existente = new Categoria("Romance", "Descrição antiga");
        existente.setId(10L);
        Livro livro = new Livro();
        existente.getLivros().add(livro);
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.save(any(Categoria.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        Categoria atualizada = categoriaService.atualizar(10L,
                new Categoria("  Literatura  ", "Nova descrição"));

        assertThat(atualizada.getId()).isEqualTo(10L);
        assertThat(atualizada.getNome()).isEqualTo("Literatura");
        assertThat(atualizada.getDescricao()).isEqualTo("Nova descrição");
        assertThat(atualizada.getLivros()).containsExactly(livro);
    }

    @Test
    void devePermitirAtualizarMantendoOProprioNome() {
        Categoria existente = new Categoria("Romance", "Descrição antiga");
        existente.setId(10L);
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.save(any(Categoria.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        Categoria atualizada = categoriaService.atualizar(10L,
                new Categoria("Romance", "Nova descrição"));

        assertThat(atualizada.getNome()).isEqualTo("Romance");
        assertThat(atualizada.getDescricao()).isEqualTo("Nova descrição");
    }

    @Test
    void naoDeveAtualizarComNomeDeOutraCategoria() {
        Categoria existente = new Categoria("Romance", "Descrição antiga");
        existente.setId(10L);
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.existsByNomeIgnoreCase("Fantasia")).thenReturn(true);

        Throwable erro = catchThrowable(() -> categoriaService.atualizar(10L,
                new Categoria("Fantasia", "Nova descrição")));

        assertThat(erro).isInstanceOf(IllegalArgumentException.class);

        verify(categoriaRepository, never()).save(any(Categoria.class));
        assertThat(existente.getNome()).isEqualTo("Romance");
        assertThat(existente.getDescricao()).isEqualTo("Descrição antiga");
    }

    @Test
    void naoDeveAtualizarComNomeVazio() {
        Categoria existente = new Categoria("Romance", null);
        existente.setId(10L);
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(existente));

        Throwable erro = catchThrowable(() -> categoriaService.atualizar(10L, new Categoria("  ", null)));

        assertThat(erro).isInstanceOf(IllegalArgumentException.class);

        verify(categoriaRepository, never()).save(any(Categoria.class));
        assertThat(existente.getNome()).isEqualTo("Romance");
    }

    @Test
    void naoDeveAtualizarCategoriaInexistente() {
        when(categoriaRepository.findById(999L)).thenReturn(Optional.empty());

        Throwable erro = catchThrowable(() -> categoriaService.atualizar(999L,
                new Categoria("Romance", null)));

        assertThat(erro).isInstanceOf(EntityNotFoundException.class);

        verify(categoriaRepository, never()).save(any(Categoria.class));
    }

    @Test
    void deveExcluirCategoriaExistente() {
        Categoria existente = new Categoria("Romance", null);
        existente.setId(10L);
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(existente));

        categoriaService.excluir(10L);

        verify(categoriaRepository).delete(existente);
    }

    @Test
    void naoDeveExcluirCategoriaInexistente() {
        when(categoriaRepository.findById(999L)).thenReturn(Optional.empty());

        Throwable erro = catchThrowable(() -> categoriaService.excluir(999L));

        assertThat(erro).isInstanceOf(EntityNotFoundException.class);

        verify(categoriaRepository, never()).delete(any(Categoria.class));
    }

}
