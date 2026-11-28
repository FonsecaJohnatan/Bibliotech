package br.edu.bibliotech.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import br.edu.bibliotech.model.Autor;
import br.edu.bibliotech.model.Categoria;
import br.edu.bibliotech.model.Livro;
import br.edu.bibliotech.repository.AutorRepository;
import br.edu.bibliotech.repository.CategoriaRepository;
import br.edu.bibliotech.repository.LivroRepository;

@SpringBootTest(properties = {
	"spring.datasource.url=jdbc:h2:mem:livros-web;DB_CLOSE_DELAY=-1",
	"spring.jpa.hibernate.ddl-auto=create-drop",
	"spring.jpa.open-in-view=false",
	"spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc(addFilters = false)
class LivroControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private LivroRepository livroRepository;

	@Autowired
	private CategoriaRepository categoriaRepository;

	@Autowired
	private AutorRepository autorRepository;

	private Categoria categoria;
	private Autor autor;

	@BeforeEach
	public void preparar() {
		livroRepository.deleteAll();
		autorRepository.deleteAll();
		categoriaRepository.deleteAll();
		categoria = categoriaRepository.save(new Categoria("Romance", null));
		autor = autorRepository.save(new Autor("Machado de Assis", null, null));
	}

	private Livro salvar() {
		Livro livro = new Livro("Dom Casmurro", "9780110002224", 1899, categoria);
		livro.getAutores().add(autor);
		return livroRepository.save(livro);
	}

	@Test
	public void deveAbrirFormularioDeLivro() throws Exception {
		mockMvc.perform(get("/livro/formulario"))
			.andExpect(status().isOk())
			.andExpect(view().name("livro/formulario"))
			.andExpect(content().string(containsString("action=\"/livro/novo\"")))
			.andExpect(content().string(containsString("Romance")))
			.andExpect(content().string(containsString("Machado de Assis")));
	}

	@Test
	public void deveCadastrarLivroValido() throws Exception {
		Autor outro = autorRepository.save(new Autor("Segundo autor", null, null));
		mockMvc.perform(post("/livro/novo").param("titulo", "Livro MVC")
				.param("isbn", "978-0-11000-222-4").param("anoPublicacao", "2020")
				.param("categoriaId", categoria.getId().toString())
				.param("autoresIds", autor.getId().toString(), outro.getId().toString()))
			.andExpect(redirectedUrl("/home"));

		Livro livro = livroRepository.findAll().get(0);
		assertThat(livro.getAutores()).hasSize(2);
		assertThat(livro.getAnoPublicacao()).isEqualTo(2020);
		mockMvc.perform(get("/livro"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Livro MVC")))
			.andExpect(content().string(containsString("Segundo autor")));
		mockMvc.perform(get("/home"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Livro MVC")));
	}

	@Test
	public void deveReexibirFormularioQuandoLivroInvalido() throws Exception {
		mockMvc.perform(post("/livro/novo").param("titulo", "")
				.param("isbn", "").param("anoPublicacao", "abc"))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasFieldErrors("requisicaoNovoLivro", "titulo", "isbn", "categoriaId", "anoPublicacao"))
			.andExpect(content().string(containsString("Romance")))
			.andExpect(content().string(containsString("Machado de Assis")))
			.andExpect(content().string(containsString("Informe um ano válido")));
		assertThat(livroRepository.count()).isZero();
	}

	@Test
	public void deveRejeitarIsbnDuplicado() throws Exception {
		salvar();
		mockMvc.perform(post("/livro/novo").param("titulo", "Duplicado")
				.param("isbn", "9780110002224").param("categoriaId", categoria.getId().toString())
				.param("autoresIds", autor.getId().toString()))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasErrors("requisicaoNovoLivro"))
			.andExpect(content().string(containsString("ISBN já cadastrado")))
			.andExpect(content().string(containsString("<option value=\"" + categoria.getId() + "\" selected=\"selected\">Romance</option>")))
			.andExpect(content().string(containsString("<option value=\"" + autor.getId() + "\" selected=\"selected\">Machado de Assis</option>")));
		assertThat(livroRepository.count()).isEqualTo(1);
	}

	@Test
	public void deveAtualizarLivro() throws Exception {
		Livro livro = salvar();
		mockMvc.perform(get("/livro/" + livro.getId() + "/editar"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("value=\"Dom Casmurro\"")))
			.andExpect(content().string(containsString("<option value=\"" + categoria.getId() + "\" selected=\"selected\">Romance</option>")))
			.andExpect(content().string(containsString("<option value=\"" + autor.getId() + "\" selected=\"selected\">Machado de Assis</option>")));

		mockMvc.perform(post("/livro/" + livro.getId()).param("titulo", "")
				.param("isbn", "9780110002224").param("categoriaId", categoria.getId().toString()))
			.andExpect(view().name("livro/formulario"))
			.andExpect(content().string(containsString("action=\"/livro/" + livro.getId() + "\"")))
			.andExpect(content().string(containsString("Machado de Assis")));

		mockMvc.perform(post("/livro/" + livro.getId()).param("titulo", "Livro atualizado")
				.param("isbn", "9780110002224").param("categoriaId", categoria.getId().toString())
				.param("_autoresIds", "1"))
			.andExpect(redirectedUrl("/home"));
		Livro atualizado = livroRepository.findById(livro.getId()).orElseThrow();
		assertThat(atualizado.getTitulo()).isEqualTo("Livro atualizado");
		assertThat(atualizado.getAutores()).isEmpty();
	}

	@Test
	public void deveRejeitarCategoriaInexistente() throws Exception {
		mockMvc.perform(post("/livro/novo").param("titulo", "Livro")
				.param("isbn", "9780110002224").param("categoriaId", "999999"))
			.andExpect(status().isOk())
			.andExpect(view().name("livro/formulario"))
			.andExpect(content().string(containsString("Categoria não encontrada")))
			.andExpect(content().string(containsString("Romance")));
		assertThat(livroRepository.count()).isZero();
	}

	@Test
	public void deveRetornar404QuandoLivroNaoExiste() throws Exception {
		mockMvc.perform(get("/livro/999999/editar"))
			.andExpect(status().isNotFound()).andExpect(view().name("erro"));
		mockMvc.perform(post("/livro/999999").param("titulo", "Livro")
				.param("isbn", "9780110002224").param("categoriaId", categoria.getId().toString()))
			.andExpect(status().isNotFound()).andExpect(view().name("erro"));
	}

	@Test
	public void deveDesabilitarFormularioSemCategoria() throws Exception {
		categoriaRepository.deleteAll();
		mockMvc.perform(get("/livro/formulario"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("antes de salvar o livro")))
			.andExpect(content().string(containsString("<button type=\"submit\" disabled=\"disabled\">Salvar</button>")));
	}
}
