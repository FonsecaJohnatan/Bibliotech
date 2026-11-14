package br.edu.bibliotech.service;

import br.edu.bibliotech.model.Categoria;
import br.edu.bibliotech.repository.CategoriaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public Categoria cadastrar(Categoria dados) {
        validarCategoria(dados);

        String nome = dados.getNome().strip();
        String descricao = dados.getDescricao();

        if (categoriaRepository.existsByNomeIgnoreCase(nome)) {
            throw new IllegalArgumentException("Nome de categoria já cadastrado");
        }

        Categoria categoria = new Categoria(nome, descricao);

        return categoriaRepository.save(categoria);
    }

    public Categoria buscarPorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada: " + id));
    }

    public List<Categoria> listar() {
        return categoriaRepository.findAll();
    }

    public Categoria atualizar(Long id, Categoria dados) {
        Categoria categoria = buscarPorId(id);

        validarCategoria(dados);

        String nome = dados.getNome().strip();
        String descricao = dados.getDescricao();
        String nomeAtual = categoria.getNome();

        if (!nome.equalsIgnoreCase(nomeAtual)
                && categoriaRepository.existsByNomeIgnoreCase(nome)) {
            throw new IllegalArgumentException("Nome de categoria já cadastrado");
        }

        categoria.setNome(nome);
        categoria.setDescricao(descricao);

        return categoriaRepository.save(categoria);
    }

    public void excluir(Long id) {
        Categoria categoria = buscarPorId(id);
        categoriaRepository.delete(categoria);
    }

    private void validarCategoria(Categoria dados) {
        if (dados == null) {
            throw new IllegalArgumentException("Dados da categoria obrigatórios");
        }

        String nome = dados.getNome();
        String descricao = dados.getDescricao();
        
        if (nome == null || nome.isBlank() || nome.strip().length() > 80) {
            throw new IllegalArgumentException("Nome de categoria inválido");
        }
        if (descricao != null && descricao.length() > 255) {
            throw new IllegalArgumentException("Descrição de categoria inválida");
        }
    }
}
