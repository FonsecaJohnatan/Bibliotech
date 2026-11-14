package br.edu.bibliotech.service;

import br.edu.bibliotech.model.Autor;
import br.edu.bibliotech.model.Categoria;
import br.edu.bibliotech.model.Livro;
import br.edu.bibliotech.repository.AutorRepository;
import br.edu.bibliotech.repository.CategoriaRepository;
import br.edu.bibliotech.repository.LivroRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

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

    public Livro buscarPorId(Long id) {
        return livroRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Livro não encontrado: " + id));
    }

    public List<Livro> listar() {
        return livroRepository.findAll();
    }

    public Livro atualizar(Long id, Livro dados) {
        Livro livro = buscarPorId(id);

        validarDadosBasicos(dados);

        String titulo = dados.getTitulo().strip();
        String isbn = normalizarIsbn(dados.getIsbn());
        Integer anoPublicacao = dados.getAnoPublicacao();

        validarIsbnUnico(isbn, id);

        Categoria categoria = buscarCategoria(dados.getCategoria());
        Set<Autor> autores = buscarAutores(dados.getAutores());

        livro.setTitulo(titulo);
        livro.setIsbn(isbn);
        livro.setAnoPublicacao(anoPublicacao);
        livro.setCategoria(categoria);
        livro.setAutores(autores);

        return livroRepository.save(livro);
    }

    public void excluir(Long id) {
        Livro livro = buscarPorId(id);
        livroRepository.delete(livro);
    }

    private void validarDadosBasicos(Livro dados) {
        if (dados == null) {
            throw new IllegalArgumentException("Dados do livro obrigatórios");
        }

        String titulo = dados.getTitulo();
        String isbn = dados.getIsbn();

        if (titulo == null || titulo.isBlank() || titulo.strip().length() > 200) {
            throw new IllegalArgumentException("Título inválido");
        }
        if (isbn == null || !isbnValido(normalizarIsbn(isbn))) {
            throw new IllegalArgumentException("ISBN inválido");
        }
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
            if (referencia == null || referencia.getId() == null) {
                throw new IllegalArgumentException("Autor informado precisa estar cadastrado");
            }

            Long autorId = referencia.getId();
            Autor autor = autorRepository.findById(autorId)
                    .orElseThrow(() -> new EntityNotFoundException("Autor não encontrado: " + autorId));
            autores.add(autor);
        }
        
        return autores;
    }
}
