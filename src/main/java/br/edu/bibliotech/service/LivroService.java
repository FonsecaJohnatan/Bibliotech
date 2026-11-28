package br.edu.bibliotech.service;

import java.util.Locale;
import br.edu.bibliotech.model.Autor;
import br.edu.bibliotech.model.Categoria;
import br.edu.bibliotech.model.Livro;
import br.edu.bibliotech.repository.AutorRepository;
import br.edu.bibliotech.repository.CategoriaRepository;
import br.edu.bibliotech.repository.LivroRepository;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class LivroService {

    private final LivroRepository livroRepository;
    private final CategoriaRepository categoriaRepository;
    private final AutorRepository autorRepository;

    public LivroService(LivroRepository livroRepository, CategoriaRepository categoriaRepository, AutorRepository autorRepository) {
        this.livroRepository = livroRepository;
        this.categoriaRepository = categoriaRepository;
        this.autorRepository = autorRepository;
    }

    @Transactional
    public Livro cadastrar(Livro dados) {
        validarDadosBasicos(dados);

        String titulo = dados.getTitulo().strip();
        String isbn = normalizarIsbn(dados.getIsbn());
        Integer anoPublicacao = dados.getAnoPublicacao();

        validarIsbnUnico(isbn, null);

        Categoria categoria = buscarCategoria(dados.getCategoria());
        Set<Autor> autores = buscarAutores(dados.getAutores());

        Livro livro = new Livro(titulo, isbn, anoPublicacao, categoria);
        livro.getAutores().addAll(autores);

        return livroRepository.save(livro);
    }

    @Transactional(readOnly = true)
    public Livro buscarPorId(Long id) {
        Livro livro = livroRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Livro não encontrado: " + id));

        Hibernate.initialize(livro.getExemplares());
        return livro;
    }

    @Transactional(readOnly = true)
    public List<Livro> listar() {
        List<Livro> livros = livroRepository.findAll();
        livros.forEach(livro -> Hibernate.initialize(livro.getExemplares()));

        return livros;
    }

    @Transactional
    public Livro atualizar(Long id, Livro dados) {
        Livro livro = buscarPorId(id);
        validarDadosBasicos(dados);

        String isbn = normalizarIsbn(dados.getIsbn());
        validarIsbnUnico(isbn, id);

        Categoria categoria = buscarCategoria(dados.getCategoria());
        Set<Autor> autores = buscarAutores(dados.getAutores());

        preencherLivro(livro, dados, isbn, categoria, autores);

        return livroRepository.save(livro);
    }

    @Transactional
    public void excluir(Long id) {
        Livro livro = buscarPorId(id);
        livroRepository.delete(livro);
    }

    private void validarDadosBasicos(Livro dados) {
        if (dados == null) {
            throw new IllegalArgumentException("Dados do livro obrigatórios");
        }

        validarTitulo(dados.getTitulo());
        validarIsbn(dados.getIsbn());
    }

    private String normalizarIsbn(String isbn) {
        return isbn.replaceAll("[\\s-]", "");
    }

    private boolean isbnValido(String isbn) {
        if (!isbn.matches("(978|979)\\d{10}")) {
            return false;
        }

        int soma = 0;
        for (int indice = 0; indice < isbn.length(); indice++) {
            int digito = isbn.charAt(indice) - '0';
            int peso = indice % 2 == 0 ? 1 : 3;
            soma += digito * peso;
        }
        return soma % 10 == 0;
    }

    private void validarIsbnUnico(String isbn, Long id) {
        Optional<Livro> encontrado = livroRepository.findByIsbn(isbn);
        Livro livroEncontrado = encontrado.orElse(null);

        if (livroEncontrado == null) {
            return;
        }

        Long idEncontrado = livroEncontrado.getId();

        if (id == null || !Objects.equals(idEncontrado, id)) {
            throw new IllegalArgumentException("ISBN já cadastrado");
        }
    }

    private Categoria buscarCategoria(Categoria referencia) {
        if (referencia == null || referencia.getId() == null) {
            throw new IllegalArgumentException("Categoria cadastrada é obrigatória");
        }

        Long categoriaId = referencia.getId();

        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada: " + categoriaId));
    }

    private Set<Autor> buscarAutores(Set<Autor> referencias) {
        Set<Autor> autores = new HashSet<>();
        if (referencias == null) {
            return autores;
        }

        for (Autor referencia : referencias) {
            Autor autor = buscarAutor(referencia);
            autores.add(autor);
        }

        return autores;
    }

    private void preencherLivro(Livro livro, Livro dados, String isbn,
            Categoria categoria, Set<Autor> autores) {
        String titulo = dados.getTitulo().strip();

        livro.setTitulo(titulo);
        livro.setIsbn(isbn);
        livro.setAnoPublicacao(dados.getAnoPublicacao());
        livro.setCategoria(categoria);
        livro.setAutores(autores);
    }

    private void validarTitulo(String titulo) {
        boolean tituloInvalido = titulo == null || titulo.isBlank() || titulo.strip().length() > 200;

        if (tituloInvalido) {
            throw new IllegalArgumentException("Título inválido");
        }
    }

    private void validarIsbn(String isbn) {
        boolean isbnInvalido = isbn == null || !isbnValido(normalizarIsbn(isbn));

        if (isbnInvalido) {
            throw new IllegalArgumentException("ISBN inválido");
        }
    }

    private Autor buscarAutor(Autor referencia) {
        boolean autorSemCadastro = referencia == null || referencia.getId() == null;

        if (autorSemCadastro) {
            throw new IllegalArgumentException("Autor informado precisa estar cadastrado");
        }

        Long autorId = referencia.getId();

        return autorRepository.findById(autorId)
                .orElseThrow(() -> new EntityNotFoundException("Autor não encontrado: " + autorId));
    }

    @Transactional(readOnly = true)
    public List<Livro> pesquisar(String busca, Long categoriaId, boolean somenteDisponiveis) {
        String termo = busca == null ? "" : busca.strip().toLowerCase(Locale.ROOT);

        return listar().stream()
                .filter(livro -> correspondeABusca(livro, termo))
                .filter(livro -> categoriaId == null || categoriaId.equals(livro.getCategoria().getId()))
                .filter(livro -> !somenteDisponiveis || livro.qtdDisponiveis() > 0)
                .toList();
    }

    private boolean correspondeABusca(Livro livro, String termo) {
        if (termo.isBlank()) {
            return true;
        }

        boolean tituloEncontrado = livro.getTitulo().toLowerCase(Locale.ROOT).contains(termo);
        String isbnBuscado = termo.replaceAll("[\\s-]", "");
        boolean isbnEncontrado = !isbnBuscado.isBlank() && livro.getIsbn().contains(isbnBuscado);
        boolean autorEncontrado = livro.getAutores().stream()
                .anyMatch(autor -> autor.getNome().toLowerCase(Locale.ROOT).contains(termo));

        return tituloEncontrado || isbnEncontrado || autorEncontrado;
    }
}
