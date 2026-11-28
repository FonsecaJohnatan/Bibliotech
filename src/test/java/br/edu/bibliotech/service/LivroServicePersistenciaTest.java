package br.edu.bibliotech.service;

import br.edu.bibliotech.model.Autor;
import br.edu.bibliotech.model.Categoria;
import br.edu.bibliotech.model.Livro;
import br.edu.bibliotech.repository.AutorRepository;
import br.edu.bibliotech.repository.CategoriaRepository;
import br.edu.bibliotech.repository.LivroRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(LivroService.class)
class LivroServicePersistenciaTest {

    @Autowired
    private LivroService livroService;
    @Autowired
    private LivroRepository livroRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private AutorRepository autorRepository;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deveAtualizarRelacionamentosForaDaTransacaoDeLeitura() {
        Categoria romance = categoriaRepository.save(new Categoria("Romance", null));
        Autor primeiroAutor = autorRepository.save(new Autor("Primeiro autor", null, null));
        Livro dados = new Livro("Livro original", "9780110002224", 2000, romance);
        dados.getAutores().add(primeiroAutor);
        Livro salvo = livroService.cadastrar(dados);

        Categoria ciencia = categoriaRepository.save(new Categoria("Ciência", null));
        Autor segundoAutor = autorRepository.save(new Autor("Segundo autor", null, null));
        Livro alteracao = new Livro("Livro atualizado", "9780110002231", 2020, ciencia);
        alteracao.getAutores().add(segundoAutor);

        livroService.atualizar(salvo.getId(), alteracao);

        Livro persistido = livroRepository.findByIsbn("9780110002231").orElseThrow();
        assertThat(persistido.getTitulo()).isEqualTo("Livro atualizado");
        assertThat(persistido.getCategoria().getId()).isEqualTo(ciencia.getId());
        assertThat(livroRepository.findByAutoresId(segundoAutor.getId(), PageRequest.of(0, 10))).extracting(Livro::getId).contains(salvo.getId());
        assertThat(livroRepository.findByAutoresId(primeiroAutor.getId(), PageRequest.of(0, 10))).isEmpty();
    }

}
