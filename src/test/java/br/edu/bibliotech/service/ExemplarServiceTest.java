package br.edu.bibliotech.service;

import jakarta.persistence.EntityNotFoundException;

import br.edu.bibliotech.model.Exemplar;
import br.edu.bibliotech.model.Livro;
import br.edu.bibliotech.model.StatusExemplar;
import br.edu.bibliotech.repository.EmprestimoRepository;
import br.edu.bibliotech.repository.ExemplarRepository;
import br.edu.bibliotech.repository.LivroRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.EnumSource;
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
class ExemplarServiceTest {

    @Mock
    private ExemplarRepository exemplarRepository;
    @Mock
    private LivroRepository livroRepository;
    @Mock
    private EmprestimoRepository emprestimoRepository;
    @InjectMocks
    private ExemplarService exemplarService;

    @Test
    void deveCadastrarExemplarValido() {
        Livro livro = new Livro();
        livro.setId(10L);

        Exemplar dados = new Exemplar(" tombo-1 ", livro);
        dados.setStatus(StatusExemplar.EMPRESTADO);

        when(livroRepository.findById(10L)).thenReturn(Optional.of(livro));
        when(exemplarRepository.save(any(Exemplar.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        Exemplar salvo = exemplarService.cadastrar(dados);

        assertThat(salvo.getCodigoTombo()).isEqualTo("TOMBO-1");
        assertThat(salvo.getLivro()).isSameAs(livro);
        assertThat(salvo.getStatus()).isEqualTo(StatusExemplar.DISPONIVEL);

        verify(exemplarRepository).save(any(Exemplar.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void naoDeveCadastrarCodigoVazio(String codigo) {
        Throwable erro = catchThrowable(() -> exemplarService.cadastrar(new Exemplar(codigo, null)));

        assertThat(erro).isInstanceOf(IllegalArgumentException.class);

        verify(exemplarRepository, never()).save(any(Exemplar.class));
    }

    @Test
    void naoDeveCadastrarCodigoAcimaDoLimite() {
        Throwable erro = catchThrowable(() -> exemplarService.cadastrar(new Exemplar("A".repeat(21), null)));

        assertThat(erro).isInstanceOf(IllegalArgumentException.class);

        verify(exemplarRepository, never()).save(any(Exemplar.class));
    }

    @Test
    void naoDeveCadastrarCodigoDuplicadoIgnorandoMaiusculas() {
        Exemplar existente = new Exemplar("TOMBO-1", null);
        when(exemplarRepository.findByCodigoTombo("TOMBO-1")).thenReturn(Optional.of(existente));

        Throwable erro = catchThrowable(() -> exemplarService.cadastrar(new Exemplar("tombo-1", null)));

        assertThat(erro).isInstanceOf(IllegalArgumentException.class);

        verify(exemplarRepository, never()).save(any(Exemplar.class));
    }

    @Test
    void naoDeveCadastrarSemLivro() {
        Throwable erro = catchThrowable(() -> exemplarService.cadastrar(new Exemplar("TOMBO-1", null)));

        assertThat(erro).isInstanceOf(IllegalArgumentException.class);

        verify(exemplarRepository, never()).save(any(Exemplar.class));
    }

    @Test
    void naoDeveCadastrarLivroInexistente() {
        Livro livro = new Livro();
        livro.setId(999L);
        when(livroRepository.findById(999L)).thenReturn(Optional.empty());

        Throwable erro = catchThrowable(() -> exemplarService.cadastrar(new Exemplar("TOMBO-1", livro)));

        assertThat(erro).isInstanceOf(EntityNotFoundException.class);

        verify(exemplarRepository, never()).save(any(Exemplar.class));
    }

    @Test
    void deveBuscarExemplarPorId() {
        Exemplar existente = new Exemplar("TOMBO-1", null);
        existente.setId(20L);

        when(exemplarRepository.findById(20L)).thenReturn(Optional.of(existente));

        var resultado = exemplarService.buscarPorId(20L);

        assertThat(resultado).isSameAs(existente);
    }

    @Test
    void naoDeveBuscarExemplarInexistente() {
        when(exemplarRepository.findById(999L)).thenReturn(Optional.empty());

        Throwable erro = catchThrowable(() -> exemplarService.buscarPorId(999L));

        assertThat(erro).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deveListarExemplaresDoLivro() {
        Livro livro = new Livro();
        livro.setId(10L);

        Exemplar primeiro = new Exemplar("TOMBO-1", livro);
        Exemplar segundo = new Exemplar("TOMBO-2", livro);

        when(livroRepository.existsById(10L)).thenReturn(true);
        when(exemplarRepository.findByLivroId(10L)).thenReturn(List.of(primeiro, segundo));

        var resultado = exemplarService.listarPorLivro(10L);

        assertThat(resultado).containsExactly(primeiro, segundo);
    }

    @ParameterizedTest
    @EnumSource(StatusExemplar.class)
    void deveConsultarStatus(StatusExemplar status) {
        Exemplar exemplar = new Exemplar("TOMBO-1", null);
        exemplar.setId(20L);
        exemplar.setStatus(status);

        when(exemplarRepository.findById(20L)).thenReturn(Optional.of(exemplar));

        var resultado = exemplarService.consultarStatus(20L);

        assertThat(resultado).isEqualTo(status);
    }

    @Test
    void deveAtualizarCodigoELivroSemAlterarStatus() {
        Livro antigo = new Livro();
        antigo.setId(10L);
        Livro novo = new Livro();
        novo.setId(11L);
        Exemplar existente = new Exemplar("TOMBO-1", antigo);
        existente.setId(20L);
        existente.setStatus(StatusExemplar.INDISPONIVEL);
        Exemplar dados = new Exemplar(" tombo-2 ", novo);
        dados.setStatus(StatusExemplar.DISPONIVEL);

        when(exemplarRepository.findById(20L)).thenReturn(Optional.of(existente));
        when(livroRepository.findById(11L)).thenReturn(Optional.of(novo));
        when(exemplarRepository.save(any(Exemplar.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        Exemplar atualizado = exemplarService.atualizar(20L, dados);

        assertThat(atualizado.getCodigoTombo()).isEqualTo("TOMBO-2");
        assertThat(atualizado.getLivro()).isSameAs(novo);
        assertThat(atualizado.getStatus()).isEqualTo(StatusExemplar.INDISPONIVEL);
    }

    @Test
    void naoDeveAtualizarComCodigoDeOutroExemplar() {
        Exemplar existente = new Exemplar("TOMBO-1", null);
        existente.setId(20L);
        Exemplar outro = new Exemplar("TOMBO-2", null);
        outro.setId(21L);

        when(exemplarRepository.findById(20L)).thenReturn(Optional.of(existente));
        when(exemplarRepository.findByCodigoTombo("TOMBO-2")).thenReturn(Optional.of(outro));

        Throwable erro = catchThrowable(() -> exemplarService.atualizar(20L, new Exemplar("TOMBO-2", null)));

        assertThat(erro).isInstanceOf(IllegalArgumentException.class);

        assertThat(existente.getCodigoTombo()).isEqualTo("TOMBO-1");
        verify(exemplarRepository, never()).save(any(Exemplar.class));
    }

    @Test
    void naoDeveMoverExemplarComHistoricoParaOutroLivro() {
        Livro antigo = new Livro();
        antigo.setId(10L);
        Livro novo = new Livro();
        novo.setId(11L);
        Exemplar existente = new Exemplar("TOMBO-1", antigo);
        existente.setId(20L);

        when(exemplarRepository.findById(20L)).thenReturn(Optional.of(existente));
        when(emprestimoRepository.existsByExemplarId(20L)).thenReturn(true);

        Throwable erro = catchThrowable(() -> exemplarService.atualizar(20L, new Exemplar("TOMBO-1", novo)));

        assertThat(erro).isInstanceOf(IllegalStateException.class);

        verify(exemplarRepository, never()).save(any(Exemplar.class));

        assertThat(existente.getLivro()).isSameAs(antigo);
    }

    @Test
    void deveExcluirExemplarSemHistorico() {
        Exemplar existente = new Exemplar("TOMBO-1", null);
        existente.setId(20L);
        when(exemplarRepository.findById(20L)).thenReturn(Optional.of(existente));

        exemplarService.excluir(20L);

        verify(exemplarRepository).delete(existente);
    }

    @Test
    void naoDeveExcluirExemplarEmprestado() {
        Exemplar existente = new Exemplar("TOMBO-1", null);
        existente.setId(20L);
        existente.setStatus(StatusExemplar.EMPRESTADO);
        when(exemplarRepository.findById(20L)).thenReturn(Optional.of(existente));

        Throwable erro = catchThrowable(() -> exemplarService.excluir(20L));

        assertThat(erro).isInstanceOf(IllegalStateException.class);

        verify(exemplarRepository, never()).delete(any(Exemplar.class));
    }

    @Test
    void naoDeveExcluirExemplarComHistorico() {
        Exemplar existente = new Exemplar("TOMBO-1", null);
        existente.setId(20L);

        when(exemplarRepository.findById(20L)).thenReturn(Optional.of(existente));
        when(emprestimoRepository.existsByExemplarId(20L)).thenReturn(true);

        Throwable erro = catchThrowable(() -> exemplarService.excluir(20L));

        assertThat(erro).isInstanceOf(IllegalStateException.class);

        verify(exemplarRepository, never()).delete(any(Exemplar.class));
    }

}
