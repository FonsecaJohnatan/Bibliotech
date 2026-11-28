package br.edu.bibliotech.service;

import br.edu.bibliotech.dto.VerificacaoEmprestimo;
import br.edu.bibliotech.model.Emprestimo;
import br.edu.bibliotech.model.Exemplar;
import br.edu.bibliotech.model.Usuario;
import br.edu.bibliotech.repository.EmprestimoRepository;
import br.edu.bibliotech.repository.ExemplarRepository;
import br.edu.bibliotech.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public Emprestimo registrar(Long usuarioId, Long exemplarId) {
        return registrar(usuarioId, exemplarId, LocalDate.now());
    }

    @Transactional
    public Emprestimo registrar(Long usuarioId, Long exemplarId, LocalDate dataEmprestimo) {
        validarDataEmprestimo(dataEmprestimo);

        Usuario usuario = buscarUsuario(usuarioId);
        validarUsuarioAtivo(usuario);
        validarLimiteDeEmprestimos(usuarioId);
        validarAusenciaDeAtrasos(usuarioId, dataEmprestimo);

        Exemplar exemplar = buscarExemplar(exemplarId);
        exemplar.emprestar();

        Emprestimo emprestimo = new Emprestimo(exemplar, usuario, dataEmprestimo);

        exemplarRepository.save(exemplar);

        return emprestimoRepository.save(emprestimo);
    }

    public Emprestimo buscarPorId(Long id) {
        return emprestimoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Empréstimo não encontrado: " + id));
    }

    @Transactional
    public Emprestimo devolver(Long id) {
        return devolver(id, LocalDate.now());
    }

    @Transactional
    public Emprestimo devolver(Long id, LocalDate dataDevolucao) {
        Emprestimo emprestimo = buscarPorId(id);

        validarDevolucao(emprestimo, dataDevolucao);

        BigDecimal multa = emprestimo.calcularMulta(dataDevolucao);
        Exemplar exemplar = emprestimo.getExemplar();

        emprestimo.setDataDevolucao(dataDevolucao);
        emprestimo.setMulta(multa);

        exemplar.devolver();

        exemplarRepository.save(exemplar);

        return emprestimoRepository.save(emprestimo);
    }

    @Transactional
    public Emprestimo renovar(Long id) {
        return renovar(id, LocalDate.now());
    }

    @Transactional
    public Emprestimo renovar(Long id, LocalDate hoje) {
        Emprestimo emprestimo = buscarPorId(id);

        if (emprestimo.estaAtrasado(hoje)) {
            throw new IllegalStateException("Empréstimo atrasado não pode ser renovado");
        }

        emprestimo.renovar();

        return emprestimoRepository.save(emprestimo);
    }

    public List<Emprestimo> listar() {
        return emprestimoRepository.findAll();
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

    private void validarDataEmprestimo(LocalDate dataEmprestimo) {
        if (dataEmprestimo == null) {
            throw new IllegalArgumentException("Data do empréstimo obrigatória");
        }
    }

    private Usuario buscarUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + usuarioId));
    }

    private Exemplar buscarExemplar(Long exemplarId) {
        return exemplarRepository.findById(exemplarId)
                .orElseThrow(() -> new EntityNotFoundException("Exemplar não encontrado: " + exemplarId));
    }

    private void validarUsuarioAtivo(Usuario usuario) {
        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new IllegalStateException("Usuário inativo não pode emprestar");
        }
    }

    private void validarLimiteDeEmprestimos(Long usuarioId) {
        long quantidadeAtiva = emprestimoRepository.countByUsuarioIdAndDataDevolucaoIsNull(usuarioId);
        boolean limiteAtingido = quantidadeAtiva >= Usuario.LIMITE_EMPRESTIMOS_ATIVOS;

        if (limiteAtingido) {
            throw new IllegalStateException("Limite de empréstimos ativos atingido");
        }
    }

    private void validarDevolucao(Emprestimo emprestimo, LocalDate dataDevolucao) {
        if (emprestimo.getDataDevolucao() != null) {
            throw new IllegalStateException("Empréstimo já devolvido");
        }

        boolean dataInvalida = dataDevolucao == null
                || dataDevolucao.isBefore(emprestimo.getDataEmprestimo());

        if (dataInvalida) {
            throw new IllegalArgumentException("Data de devolução inválida");
        }
    }

    public VerificacaoEmprestimo verificarUsuario(Long usuarioId) {
        Usuario usuario = buscarUsuario(usuarioId);
        LocalDate hoje = LocalDate.now();
        boolean semAtraso = listarAtrasadosPorUsuario(usuarioId, hoje).isEmpty();
        boolean semMulta = multaPendente(usuarioId).signum() == 0;
        long quantidade = emprestimoRepository.countByUsuarioIdAndDataDevolucaoIsNull(usuarioId);

        return new VerificacaoEmprestimo(
                Boolean.TRUE.equals(usuario.getAtivo()), semMulta, semAtraso,
                quantidade < Usuario.LIMITE_EMPRESTIMOS_ATIVOS, hoje.plusDays(Emprestimo.PRAZO_DIAS));
    }

    public BigDecimal multaPendente(Long usuarioId) {
        LocalDate hoje = LocalDate.now();

        return listarAtrasadosPorUsuario(usuarioId, hoje).stream()
                .map(emprestimo -> emprestimo.calcularMulta(hoje))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public Emprestimo renovarDoUsuario(Long id, Long usuarioId) {
        Emprestimo emprestimo = buscarPorId(id);

        if (!usuarioId.equals(emprestimo.getUsuario().getId())) {
            throw new IllegalStateException("Este empréstimo pertence a outro usuário");
        }

        return renovar(id);
    }

    public List<Emprestimo> filtrar(Long usuarioId, String situacao) {
        List<Emprestimo> emprestimos = usuarioId == null ? listar() : listarPorUsuario(usuarioId);
        LocalDate hoje = LocalDate.now();

        return emprestimos.stream()
                .filter(emprestimo -> correspondeASituacao(emprestimo, situacao, hoje))
                .toList();
    }

    private boolean correspondeASituacao(Emprestimo emprestimo, String situacao, LocalDate hoje) {
        return switch (situacao) {
            case "ABERTOS" -> emprestimo.getDataDevolucao() == null;
            case "ATRASADOS" -> emprestimo.estaAtrasado(hoje);
            case "DEVOLVIDOS" -> emprestimo.getDataDevolucao() != null;
            default -> true;
        };
    }

    private void validarAusenciaDeAtrasos(Long usuarioId, LocalDate hoje) {
        if (!listarAtrasadosPorUsuario(usuarioId, hoje).isEmpty()) {
            throw new IllegalStateException("Usuário com empréstimo atrasado e multa pendente");
        }
    }
}
