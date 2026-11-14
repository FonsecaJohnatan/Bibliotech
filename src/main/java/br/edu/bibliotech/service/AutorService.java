package br.edu.bibliotech.service;

import br.edu.bibliotech.model.Autor;
import br.edu.bibliotech.repository.AutorRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AutorService {

    private final AutorRepository autorRepository;

    public AutorService(AutorRepository autorRepository) {
        this.autorRepository = autorRepository;
    }

    public Autor cadastrar(Autor dados) {
        validarAutor(dados);

        String nome = dados.getNome().strip();
        String nacionalidade = dados.getNacionalidade();
        LocalDate dataNascimento = dados.getDataNascimento();

        Autor autor = new Autor(nome, nacionalidade, dataNascimento);

        return autorRepository.save(autor);
    }

    public Autor buscarPorId(Long id) {
        return autorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Autor não encontrado: " + id));
    }

    public List<Autor> listar() {
        return autorRepository.findAll();
    }

    public Autor atualizar(Long id, Autor dados) {
        Autor autor = buscarPorId(id);
        validarAutor(dados);

        String nome = dados.getNome().strip();
        String nacionalidade = dados.getNacionalidade();
        LocalDate dataNascimento = dados.getDataNascimento();

        autor.setNome(nome);
        autor.setNacionalidade(nacionalidade);
        autor.setDataNascimento(dataNascimento);
        
        return autorRepository.save(autor);
    }

    public void excluir(Long id) {
        Autor autor = buscarPorId(id);
        autorRepository.delete(autor);
    }

    private void validarAutor(Autor dados) {
        if (dados == null) {
            throw new IllegalArgumentException("Dados do autor obrigatórios");
        }

        String nome = dados.getNome();
        String nacionalidade = dados.getNacionalidade();

        if (nome == null || nome.isBlank() || nome.strip().length() > 120) {
            throw new IllegalArgumentException("Nome inválido");
        }
        if (nacionalidade != null && nacionalidade.length() > 60) {
            throw new IllegalArgumentException("Nacionalidade inválida");
        }
    }
}
