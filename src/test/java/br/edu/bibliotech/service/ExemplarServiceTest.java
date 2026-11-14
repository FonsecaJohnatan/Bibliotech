package br.edu.bibliotech.service;

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
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExemplarServiceTest {

    @Mock private ExemplarRepository exemplarRepository;
    @Mock private LivroRepository livroRepository;
    @Mock private EmprestimoRepository emprestimoRepository;
    @InjectMocks private ExemplarService exemplarService;

    @Test
    void deveCadastrarExemplarValido() {
        Livro livro = new Livro();
        livro.setId(10L);
        
        Exemplar dados = new Exemplar(" tombo-1 ", livro);
        dados.setStatus(StatusExemplar.EMPRESTADO);

        when(livroRepository.findById(10L)).thenReturn(Optional.of(livro));
        when(exemplarRepository.save(any(Exemplar.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

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
        assertThatThrownBy(() -> exemplarService.cadastrar(new Exemplar(codigo, null)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(exemplarRepository, never()).save(any(Exemplar.class));
    }

    @Test
    void naoDeveCadastrarCodigoAcimaDoLimite() {
        assertThatThrownBy(() -> exemplarService.cadastrar(new Exemplar("A".repeat(21), null)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(exemplarRepository, never()).save(any(Exemplar.class));
    }

    @Test
    void naoDeveCadastrarCodigoDuplicadoIgnorandoMaiusculas() {
        Exemplar existente = new Exemplar("TOMBO-1", null);
        when(exemplarRepository.findByCodigoTombo("TOMBO-1")).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> exemplarService.cadastrar(new Exemplar("tombo-1", null)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(exemplarRepository, never()).save(any(Exemplar.class));
    }

    @Test
    void naoDeveCadastrarSemLivro() {
        assertThatThrownBy(() -> exemplarService.cadastrar(new Exemplar("TOMBO-1", null)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(exemplarRepository, never()).save(any(Exemplar.class));
    }

    @Test
    void naoDeveCadastrarLivroInexistente() {
        Livro livro = new Livro();
        livro.setId(999L);
        when(livroRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exemplarService.cadastrar(new Exemplar("TOMBO-1", livro)))
                .isInstanceOf(RuntimeException.class);

        verify(exemplarRepository, never()).save(any(Exemplar.class));
    }

    @Test
    void deveBuscarExemplarPorId() {
        Exemplar existente = new Exemplar("TOMBO-1", null);
        existente.setId(20L);

        when(exemplarRepository.findById(20L)).thenReturn(Optional.of(existente));

        assertThat(exemplarService.buscarPorId(20L)).isSameAs(existente);
    }

    @Test
    void naoDeveBuscarExemplarInexistente() {
        when(exemplarRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exemplarService.buscarPorId(999L)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void deveListarExemplaresDoLivro() {
        Livro livro = new Livro();
        livro.setId(10L);

        Exemplar primeiro = new Exemplar("TOMBO-1", livro);
        Exemplar segundo = new Exemplar("TOMBO-2", livro);

        when(livroRepository.existsById(10L)).thenReturn(true);
        when(exemplarRepository.findByLivroId(10L)).thenReturn(List.of(primeiro, segundo));

        assertThat(exemplarService.listarPorLivro(10L)).containsExactly(primeiro, segundo);
    }

    @ParameterizedTest
    @ValueSource(strings = {"DISPONIVEL", "EMPRESTADO", "INDISPONIVEL"})
    void deveConsultarStatus(String status) {
        Exemplar exemplar = new Exemplar("TOMBO-1", null);
        exemplar.setId(20L);
        exemplar.setStatus(StatusExemplar.valueOf(status));

        when(exemplarRepository.findById(20L)).thenReturn(Optional.of(exemplar));

        assertThat(exemplarService.consultarStatus(20L)).isEqualTo(StatusExemplar.valueOf(status));
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
        when(exemplarRepository.save(any(Exemplar.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

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

        assertThatThrownBy(() -> exemplarService.atualizar(20L, new Exemplar("TOMBO-2", null)))
                .isInstanceOf(IllegalArgumentException.class);

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

        assertThatThrownBy(() -> exemplarService.atualizar(20L, new Exemplar("TOMBO-1", novo)))
                .isInstanceOf(IllegalStateException.class);

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

        assertThatThrownBy(() -> exemplarService.excluir(20L)).isInstanceOf(IllegalStateException.class);

        verify(exemplarRepository, never()).delete(any(Exemplar.class));
    }

    @Test
    void naoDeveExcluirExemplarComHistorico() {
        Exemplar existente = new Exemplar("TOMBO-1", null);
        existente.setId(20L);

        when(exemplarRepository.findById(20L)).thenReturn(Optional.of(existente));
        when(emprestimoRepository.existsByExemplarId(20L)).thenReturn(true);

        assertThatThrownBy(() -> exemplarService.excluir(20L)).isInstanceOf(IllegalStateException.class);

        verify(exemplarRepository, never()).delete(any(Exemplar.class));
    }
}
