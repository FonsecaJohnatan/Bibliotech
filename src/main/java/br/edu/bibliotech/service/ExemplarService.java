package br.edu.bibliotech.service;

import br.edu.bibliotech.model.Exemplar;
import br.edu.bibliotech.model.Livro;
import br.edu.bibliotech.model.StatusExemplar;
import br.edu.bibliotech.repository.EmprestimoRepository;
import br.edu.bibliotech.repository.ExemplarRepository;
import br.edu.bibliotech.repository.LivroRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
public class ExemplarService {

    private final ExemplarRepository exemplarRepository;
    private final LivroRepository livroRepository;
    private final EmprestimoRepository emprestimoRepository;

    public ExemplarService(ExemplarRepository exemplarRepository, LivroRepository livroRepository, EmprestimoRepository emprestimoRepository) {
        this.exemplarRepository = exemplarRepository;
        this.livroRepository = livroRepository;
        this.emprestimoRepository = emprestimoRepository;
    }

    @Transactional
    public Exemplar cadastrar(Exemplar dados) {
        validarDados(dados);

        String codigoTombo = normalizarCodigo(dados.getCodigoTombo());

        validarCodigoUnico(codigoTombo, null);

        Livro livro = buscarLivro(dados.getLivro());

        Exemplar exemplar = new Exemplar(codigoTombo, livro);

        return exemplarRepository.save(exemplar);
    }

    public Exemplar buscarPorId(Long id) {
        return exemplarRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Exemplar não encontrado: " + id));
    }

    public List<Exemplar> listar() {
        return exemplarRepository.findAll();
    }

    public List<Exemplar> listarPorLivro(Long livroId) {
        if (livroId == null || !livroRepository.existsById(livroId)) {
            throw new EntityNotFoundException("Livro não encontrado: " + livroId);
        }
        return exemplarRepository.findByLivroId(livroId);
    }

    public StatusExemplar consultarStatus(Long id) {
        Exemplar exemplar = buscarPorId(id);

        return exemplar.getStatus();
    }

    @Transactional
    public Exemplar atualizar(Long id, Exemplar dados) {
        Exemplar exemplar = buscarPorId(id);

        validarDados(dados);

        String codigoTombo = normalizarCodigo(dados.getCodigoTombo());

        validarCodigoUnico(codigoTombo, id);

        Livro livroInformado = dados.getLivro();
        validarMudancaDeLivro(id, exemplar, livroInformado);

        Livro livro = buscarLivro(livroInformado);

        exemplar.setCodigoTombo(codigoTombo);
        exemplar.setLivro(livro);

        return exemplarRepository.save(exemplar);
    }

    public void excluir(Long id) {
        Exemplar exemplar = buscarPorId(id);
        StatusExemplar status = exemplar.getStatus();

        boolean exclusaoBloqueada = status == StatusExemplar.EMPRESTADO
                || emprestimoRepository.existsByExemplarId(id);

        if (exclusaoBloqueada) {
            throw new IllegalStateException("Exemplar emprestado ou com histórico não pode ser excluído");
        }

        exemplarRepository.delete(exemplar);
    }

    private void validarDados(Exemplar dados) {
        if (dados == null) {
            throw new IllegalArgumentException("Dados do exemplar obrigatórios");
        }

        String codigoTombo = dados.getCodigoTombo();

        boolean codigoInvalido = codigoTombo == null || codigoTombo.isBlank()
                || codigoTombo.strip().length() > 20;

        if (codigoInvalido) {
            throw new IllegalArgumentException("Código de tombo inválido");
        }
    }

    private String normalizarCodigo(String codigoTombo) {
        return codigoTombo.strip().toUpperCase(Locale.ROOT);
    }

    private void validarCodigoUnico(String codigoTombo, Long id) {
        Optional<Exemplar> encontrado = exemplarRepository.findByCodigoTombo(codigoTombo);

        Exemplar exemplarEncontrado = encontrado.orElse(null);

        if (exemplarEncontrado == null) {
            return;
        }

        Long idEncontrado = exemplarEncontrado.getId();

        if (id == null || !Objects.equals(idEncontrado, id)) {
            throw new IllegalArgumentException("Código de tombo já cadastrado");
        }
    }

    private Livro buscarLivro(Livro referencia) {
        if (referencia == null) {
            throw new IllegalArgumentException("Livro cadastrado é obrigatório");
        }

        Long livroId = referencia.getId();

        if (livroId == null) {
            throw new IllegalArgumentException("Livro cadastrado é obrigatório");
        }
        return livroRepository.findById(livroId)
                .orElseThrow(() -> new EntityNotFoundException("Livro não encontrado: " + livroId));
    }

    private void validarMudancaDeLivro(Long exemplarId, Exemplar exemplar, Livro livroInformado) {
        Long livroAtualId = obterIdLivro(exemplar.getLivro());
        Long livroInformadoId = obterIdLivro(livroInformado);

        if (Objects.equals(livroAtualId, livroInformadoId)) {
            return;
        }

        if (emprestimoRepository.existsByExemplarId(exemplarId)) {
            throw new IllegalStateException("Exemplar com histórico não pode mudar de livro");
        }
    }

    private Long obterIdLivro(Livro livro) {
        if (livro == null) {
            return null;
        }

        return livro.getId();
    }

    @Transactional
    public Exemplar cadastrar(Exemplar dados, StatusExemplar status) {
        if (status == StatusExemplar.EMPRESTADO) {
            throw new IllegalArgumentException("O status Emprestado é definido pelo registro de um empréstimo");
        }

        Exemplar exemplar = cadastrar(dados);
        if (status != null) {
            exemplar.setStatus(status);
        }
        return exemplar;
    }

    @Transactional
    public Exemplar atualizar(Long id, Exemplar dados, StatusExemplar status) {
        Exemplar exemplar = buscarPorId(id);
        boolean mudancaDeStatus = status != null && status != exemplar.getStatus();
        boolean envolveEmprestimo = status == StatusExemplar.EMPRESTADO
                || exemplar.getStatus() == StatusExemplar.EMPRESTADO;

        if (mudancaDeStatus && envolveEmprestimo) {
            throw new IllegalStateException("Registre o empréstimo ou a devolução para alterar o status Emprestado");
        }

        Exemplar atualizado = atualizar(id, dados);
        if (status != null) {
            atualizado.setStatus(status);
        }
        return atualizado;
    }

}
