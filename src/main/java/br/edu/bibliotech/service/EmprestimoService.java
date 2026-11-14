package br.edu.bibliotech.service;

import br.edu.bibliotech.model.Emprestimo;
import br.edu.bibliotech.model.Exemplar;
import br.edu.bibliotech.model.Usuario;
import br.edu.bibliotech.repository.EmprestimoRepository;
import br.edu.bibliotech.repository.ExemplarRepository;
import br.edu.bibliotech.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class EmprestimoService {

    private final EmprestimoRepository emprestimoRepository;
    private final ExemplarRepository exemplarRepository;
    private final UsuarioRepository usuarioRepository;

    public EmprestimoService(EmprestimoRepository emprestimoRepository, ExemplarRepository exemplarRepository, UsuarioRepository usuarioRepository) {
        this.emprestimoRepository = emprestimoRepository;
        this.exemplarRepository = exemplarRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public Emprestimo registrar(Long usuarioId, Long exemplarId) {
        return registrar(usuarioId, exemplarId, LocalDate.now());
    }

    public Emprestimo registrar(Long usuarioId, Long exemplarId, LocalDate dataEmprestimo) {
        if (dataEmprestimo == null) {
            throw new IllegalArgumentException("Data do empréstimo obrigatória");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + usuarioId));
        
        Boolean ativo = usuario.getAtivo();
        
        if (!Boolean.TRUE.equals(ativo)) {
            throw new IllegalStateException("Usuário inativo não pode emprestar");
        }

        long emprestimosAtivos = emprestimoRepository.countByUsuarioIdAndDataDevolucaoIsNull(usuarioId);
        
        if (emprestimosAtivos >= Usuario.LIMITE_EMPRESTIMOS_ATIVOS) {
            throw new IllegalStateException("Limite de empréstimos ativos atingido");
        }

        Exemplar exemplar = exemplarRepository.findById(exemplarId)
                .orElseThrow(() -> new EntityNotFoundException("Exemplar não encontrado: " + exemplarId));
        exemplar.emprestar();

        Emprestimo emprestimo = new Emprestimo(exemplar, usuario, dataEmprestimo);
        
        exemplarRepository.save(exemplar);
        
        return emprestimoRepository.save(emprestimo);
    }

    public Emprestimo buscarPorId(Long id) {
        return emprestimoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Empréstimo não encontrado: " + id));
    }

    public Emprestimo devolver(Long id) {
        return devolver(id, LocalDate.now());
    }

    public Emprestimo devolver(Long id, LocalDate dataDevolucao) {
        Emprestimo emprestimo = buscarPorId(id);
        
        if (emprestimo.getDataDevolucao() != null) {
            throw new IllegalStateException("Empréstimo já devolvido");
        }
        if (dataDevolucao == null || dataDevolucao.isBefore(emprestimo.getDataEmprestimo())) {
            throw new IllegalArgumentException("Data de devolução inválida");
        }

        BigDecimal multa = emprestimo.calcularMulta(dataDevolucao);
        Exemplar exemplar = emprestimo.getExemplar();
        
        emprestimo.setDataDevolucao(dataDevolucao);
        emprestimo.setMulta(multa);
        
        exemplar.devolver();

        exemplarRepository.save(exemplar);

        return emprestimoRepository.save(emprestimo);
    }

    public Emprestimo renovar(Long id) {
        Emprestimo emprestimo = buscarPorId(id);

        emprestimo.renovar();

        return emprestimoRepository.save(emprestimo);
    }

    public List<Emprestimo> listarPorUsuario(Long usuarioId) {
        return emprestimoRepository.findByUsuarioId(usuarioId);
    }

    public List<Emprestimo> listarAtivosPorUsuario(Long usuarioId) {
        return listarPorUsuario(usuarioId).stream()
                .filter(emprestimo -> emprestimo.getDataDevolucao() == null)
                .toList();
    }

    public List<Emprestimo> listarAtrasadosPorUsuario(Long usuarioId, LocalDate hoje) {
        if (hoje == null) {
            throw new IllegalArgumentException("Data de referência obrigatória");
        }

        return listarPorUsuario(usuarioId).stream()
                .filter(emprestimo -> emprestimo.getDataDevolucao() == null)
                .filter(emprestimo -> emprestimo.getDataPrevista().isBefore(hoje))
                .toList();
    }
}
