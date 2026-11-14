package br.edu.bibliotech.service;

import br.edu.bibliotech.model.Emprestimo;
import br.edu.bibliotech.model.Exemplar;
import br.edu.bibliotech.model.Perfil;
import br.edu.bibliotech.model.StatusExemplar;
import br.edu.bibliotech.model.Usuario;
import br.edu.bibliotech.repository.EmprestimoRepository;
import br.edu.bibliotech.repository.ExemplarRepository;
import br.edu.bibliotech.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class EmprestimoServiceTest {

    @Mock private EmprestimoRepository emprestimoRepository;
    @Mock private ExemplarRepository exemplarRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @InjectMocks private EmprestimoService emprestimoService;

    @Test
    void deveRegistrarEmprestimoComPrazoDe14Dias() {
        Usuario usuario = new Usuario("Maria", "maria@email.com", "hash", Perfil.LEITOR);
        usuario.setId(10L);
        Exemplar exemplar = new Exemplar("TOMBO-1", null);
        exemplar.setId(20L);

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(exemplarRepository.findById(20L)).thenReturn(Optional.of(exemplar));
        when(emprestimoRepository.save(any(Emprestimo.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        Emprestimo salvo = emprestimoService.registrar(10L, 20L, LocalDate.of(2026, 10, 1));

        assertThat(salvo.getUsuario()).isSameAs(usuario);
        assertThat(salvo.getExemplar()).isSameAs(exemplar);
        assertThat(salvo.getDataEmprestimo()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(salvo.getDataPrevista()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(exemplar.getStatus()).isEqualTo(StatusExemplar.EMPRESTADO);

        verify(exemplarRepository).save(exemplar);
        verify(emprestimoRepository).save(any(Emprestimo.class));
    }

    @Test
    void naoDeveEmprestarParaUsuarioInexistente() {
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emprestimoService.registrar(999L, 20L, LocalDate.of(2026, 10, 1)))
                .isInstanceOf(RuntimeException.class);

        verify(emprestimoRepository, never()).save(any(Emprestimo.class));
    }

    @Test
    void naoDeveEmprestarParaUsuarioInativo() {
        Usuario usuario = new Usuario("Maria", "maria@email.com", "hash", Perfil.LEITOR);
        usuario.setId(10L);
        usuario.setAtivo(false);

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> emprestimoService.registrar(10L, 20L, LocalDate.of(2026, 10, 1)))
                .isInstanceOf(IllegalStateException.class);

        verify(emprestimoRepository, never()).save(any(Emprestimo.class));
    }

    @Test
    void naoDeveUltrapassarTresEmprestimosAtivos() {
        Usuario usuario = new Usuario("Maria", "maria@email.com", "hash", Perfil.LEITOR);
        usuario.setId(10L);

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(emprestimoRepository.countByUsuarioIdAndDataDevolucaoIsNull(10L)).thenReturn(3L);

        assertThatThrownBy(() -> emprestimoService.registrar(10L, 20L, LocalDate.of(2026, 10, 1)))
                .isInstanceOf(IllegalStateException.class);

        verify(emprestimoRepository, never()).save(any(Emprestimo.class));
    }

    @Test
    void naoDeveEmprestarExemplarInexistente() {
        Usuario usuario = new Usuario("Maria", "maria@email.com", "hash", Perfil.LEITOR);
        usuario.setId(10L);

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(exemplarRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emprestimoService.registrar(10L, 999L, LocalDate.of(2026, 10, 1)))
                .isInstanceOf(RuntimeException.class);

        verify(emprestimoRepository, never()).save(any(Emprestimo.class));
    }

    @ParameterizedTest
    @EnumSource(value = StatusExemplar.class, names = {"EMPRESTADO", "INDISPONIVEL"})
    void naoDeveEmprestarExemplarIndisponivel(StatusExemplar status) {
        Usuario usuario = new Usuario("Maria", "maria@email.com", "hash", Perfil.LEITOR);
        usuario.setId(10L);
        Exemplar exemplar = new Exemplar("TOMBO-1", null);
        exemplar.setId(20L);
        exemplar.setStatus(status);

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(exemplarRepository.findById(20L)).thenReturn(Optional.of(exemplar));

        assertThatThrownBy(() -> emprestimoService.registrar(10L, 20L, LocalDate.of(2026, 10, 1)))
                .isInstanceOf(IllegalStateException.class);

        verify(emprestimoRepository, never()).save(any(Emprestimo.class));

        assertThat(exemplar.getStatus()).isEqualTo(status);
    }

    @Test
    void deveDevolverECalcularMultaDeTresDias() {
        Exemplar exemplar = new Exemplar("TOMBO-1", null);
        exemplar.setStatus(StatusExemplar.EMPRESTADO);
        Emprestimo emprestimo = new Emprestimo(exemplar, new Usuario(), LocalDate.of(2026, 10, 1));
        emprestimo.setId(30L);

        when(emprestimoRepository.findById(30L)).thenReturn(Optional.of(emprestimo));
        when(emprestimoRepository.save(any(Emprestimo.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        Emprestimo devolvido = emprestimoService.devolver(30L, LocalDate.of(2026, 10, 18));

        assertThat(devolvido.getDataDevolucao()).isEqualTo(LocalDate.of(2026, 10, 18));
        assertThat(devolvido.getMulta()).isEqualByComparingTo(new BigDecimal("3.00"));
        assertThat(exemplar.getStatus()).isEqualTo(StatusExemplar.DISPONIVEL);

        verify(exemplarRepository).save(exemplar);
        verify(emprestimoRepository).save(emprestimo);
    }

    @Test
    void deveDevolverNoPrazoSemMulta() {
        Exemplar exemplar = new Exemplar("TOMBO-1", null);
        exemplar.setStatus(StatusExemplar.EMPRESTADO);
        Emprestimo emprestimo = new Emprestimo(exemplar, new Usuario(), LocalDate.of(2026, 10, 1));
        emprestimo.setId(30L);

        when(emprestimoRepository.findById(30L)).thenReturn(Optional.of(emprestimo));
        when(emprestimoRepository.save(any(Emprestimo.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        Emprestimo devolvido = emprestimoService.devolver(30L, LocalDate.of(2026, 10, 15));

        assertThat(devolvido.getMulta()).isEqualByComparingTo("0.00");
    }

    @Test
    void naoDeveRegistrarDevolucaoDuplicada() {
        Emprestimo emprestimo = new Emprestimo(new Exemplar(), new Usuario(), LocalDate.of(2026, 10, 1));
        emprestimo.setId(30L);
        emprestimo.setDataDevolucao(LocalDate.of(2026, 10, 15));
        when(emprestimoRepository.findById(30L)).thenReturn(Optional.of(emprestimo));

        assertThatThrownBy(() -> emprestimoService.devolver(30L, LocalDate.of(2026, 10, 18)))
                .isInstanceOf(IllegalStateException.class);

        verify(emprestimoRepository, never()).save(any(Emprestimo.class));
    }

    @Test
    void deveRenovarUmaUnicaVezPorSeteDias() {
        Emprestimo emprestimo = new Emprestimo(new Exemplar(), new Usuario(), LocalDate.of(2026, 10, 1));
        emprestimo.setId(30L);

        when(emprestimoRepository.findById(30L)).thenReturn(Optional.of(emprestimo));
        when(emprestimoRepository.save(any(Emprestimo.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        Emprestimo renovado = emprestimoService.renovar(30L);

        assertThat(renovado.getDataPrevista()).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(renovado.getRenovado()).isTrue();
        assertThatThrownBy(() -> emprestimoService.renovar(30L)).isInstanceOf(IllegalStateException.class);
        
        verify(emprestimoRepository).save(emprestimo);
    }

    @Test
    void naoDeveRenovarEmprestimoDevolvido() {
        Emprestimo emprestimo = new Emprestimo(new Exemplar(), new Usuario(), LocalDate.of(2026, 10, 1));
        emprestimo.setId(30L);
        emprestimo.setDataDevolucao(LocalDate.of(2026, 10, 10));
        
        when(emprestimoRepository.findById(30L)).thenReturn(Optional.of(emprestimo));

        assertThatThrownBy(() -> emprestimoService.renovar(30L)).isInstanceOf(IllegalStateException.class);

        verify(emprestimoRepository, never()).save(any(Emprestimo.class));
    }

    @Test
    void deveListarEmprestimosDoUsuarioComTodosOsEstados() {
        Emprestimo ativo = new Emprestimo(new Exemplar(), new Usuario(), LocalDate.of(2026, 10, 1));
        Emprestimo devolvido = new Emprestimo(new Exemplar(), new Usuario(), LocalDate.of(2026, 9, 1));
        devolvido.setDataDevolucao(LocalDate.of(2026, 9, 10));
       
        when(emprestimoRepository.findByUsuarioId(10L)).thenReturn(List.of(ativo, devolvido));

        assertThat(emprestimoService.listarPorUsuario(10L)).containsExactly(ativo, devolvido);
        assertThat(emprestimoService.listarAtivosPorUsuario(10L)).containsExactly(ativo);
        assertThat(emprestimoService.listarAtrasadosPorUsuario(10L, LocalDate.of(2026, 10, 20))).containsExactly(ativo);
    }

    @Test
    void naoDeveOperarEmprestimoInexistente() {
        when(emprestimoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emprestimoService.devolver(999L, LocalDate.of(2026, 10, 18))).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> emprestimoService.renovar(999L)).isInstanceOf(RuntimeException.class);
    }
}
