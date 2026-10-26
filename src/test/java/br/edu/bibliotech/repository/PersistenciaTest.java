package br.edu.bibliotech.repository;

import br.edu.bibliotech.model.Autor;
import br.edu.bibliotech.model.Categoria;
import br.edu.bibliotech.model.Emprestimo;
import br.edu.bibliotech.model.Exemplar;
import br.edu.bibliotech.model.Livro;
import br.edu.bibliotech.model.Perfil;
import br.edu.bibliotech.model.StatusExemplar;
import br.edu.bibliotech.model.Usuario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PersistenciaTest {

    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private AutorRepository autorRepository;
    @Autowired private LivroRepository livroRepository;
    @Autowired private ExemplarRepository exemplarRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EmprestimoRepository emprestimoRepository;
    @Autowired private EntityManager em;

    private Livro livro;
    private Exemplar exemplar;
    private Usuario leitor;

    @BeforeEach
    void preparar() {
        Categoria romance = categoriaRepository.save(new Categoria("Romance", "Livros de ficção"));

        Autor machado = autorRepository.save(
                new Autor("Machado de Assis", "Brasileira", LocalDate.of(1839, 6, 21)));
        Autor coautor = autorRepository.save(new Autor("Coautor Teste", "Brasileira", null));

        livro = new Livro("Dom Casmurro", "9788535910663", 1899, romance);
        livro.getAutores().add(machado);
        livro.getAutores().add(coautor);
        livro = livroRepository.save(livro);

        exemplar = exemplarRepository.save(new Exemplar("TOMBO-0001", livro));
        exemplarRepository.save(new Exemplar("TOMBO-0002", livro));

        leitor = usuarioRepository.save(
                new Usuario("Maria Leitora", "maria@email.com", "$2a$10$hashbcrypt", Perfil.LEITOR));

        em.flush();
        em.clear();
    }

    @Test
    void persisteLivroComCategoriaEAutoresNN() {
        Livro salvo = livroRepository.findByIsbn("9788535910663").orElseThrow();

        assertThat(salvo.getCategoria().getNome()).isEqualTo("Romance");
        assertThat(salvo.getAutores()).hasSize(2);
        assertThat(salvo.getAutores()).extracting(Autor::getNome)
                .containsExactlyInAnyOrder("Machado de Assis", "Coautor Teste");
    }

    @Test
    void consultasDeLivroPorTituloCategoriaEAutor() {
        Long categoriaId = livro.getCategoria().getId();
        Long autorId = livro.getAutores().iterator().next().getId();

        assertThat(livroRepository.findByTituloContainingIgnoreCase("casmurro", PageRequest.of(0, 10)))
                .hasSize(1);
        assertThat(livroRepository.findByCategoriaId(categoriaId, PageRequest.of(0, 10))).hasSize(1);
        assertThat(livroRepository.findByAutoresId(autorId, PageRequest.of(0, 10))).hasSize(1);
    }

    @Test
    void livroPossuiVariosExemplaresDisponiveis() {
        assertThat(exemplarRepository.countByLivroIdAndStatus(livro.getId(), StatusExemplar.DISPONIVEL))
                .isEqualTo(2);
    }

    @Test
    void registraEmprestimoEMarcaExemplarComoEmprestado() {
        Exemplar ex = exemplarRepository.findById(exemplar.getId()).orElseThrow();
        Usuario u = usuarioRepository.findByEmail("maria@email.com").orElseThrow();

        ex.emprestar();
        Emprestimo emprestimo = emprestimoRepository.save(new Emprestimo(ex, u, LocalDate.of(2026, 10, 1)));
        em.flush();
        em.clear();

        Emprestimo salvo = emprestimoRepository.findById(emprestimo.getId()).orElseThrow();
        assertThat(salvo.getDataPrevista()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(salvo.getExemplar().getStatus()).isEqualTo(StatusExemplar.EMPRESTADO);
        assertThat(emprestimoRepository.findByUsuarioId(u.getId())).hasSize(1);
        assertThat(emprestimoRepository.countByUsuarioIdAndDataDevolucaoIsNull(u.getId())).isEqualTo(1);
    }

    @Test
    void encontraEmprestimosEmAtrasoECalculaMulta() {
        Exemplar ex = exemplarRepository.findById(exemplar.getId()).orElseThrow();
        Usuario u = usuarioRepository.findByEmail("maria@email.com").orElseThrow();
        Emprestimo emprestimo = emprestimoRepository.save(new Emprestimo(ex, u, LocalDate.of(2026, 9, 1)));
        em.flush();
        em.clear();

        LocalDate hoje = LocalDate.of(2026, 10, 24);
        List<Emprestimo> atrasados = emprestimoRepository.findByDataDevolucaoIsNullAndDataPrevistaBefore(hoje);

        assertThat(atrasados).hasSize(1);
        // previsto para 15/09; 24/10 = 39 dias de atraso -> R$ 39,00
        assertThat(atrasados.get(0).calcularMulta(hoje)).isEqualByComparingTo(new BigDecimal("39.00"));
    }

    @Test
    void renovacaoSoPodeSerFeitaUmaVez() {
        Emprestimo e = new Emprestimo(exemplar, leitor, LocalDate.of(2026, 10, 1));
        e.renovar();

        assertThat(e.getDataPrevista()).isEqualTo(LocalDate.of(2026, 10, 22));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, e::renovar);
    }
}
