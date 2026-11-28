package br.edu.bibliotech.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

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
class LivroRestTest {
	@Test
	void deveRetornar404QuandoListaEstaVazia() throws Exception {

		var resposta = mockMvc.perform(get("/service/livros"));

		resposta.andExpect(status().isNotFound());
	}

	@Test
	void deveRetornar404AoAtualizarLivroInexistente() throws Exception {
		String json = objectMapper.writeValueAsString(Map.of(
				"titulo", "Livro",
				"isbn", "9780110002224",
				"categoriaId", categoria.getId()));

		var resposta = mockMvc.perform(put("/service/livros/999999")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));

		resposta.andExpect(status().isNotFound());
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

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
	public void testSave() throws Exception {
		Autor outro = autorRepository.save(new Autor("Outro autor", null, null));
		String json = objectMapper.writeValueAsString(Map.of(
				"titulo", "Dom Casmurro", "isbn", "978-0-11000-222-4",
				"anoPublicacao", 1899, "categoriaId", categoria.getId(),
				"autoresIds", new Long[] {autor.getId(), outro.getId()}));

		String resposta = mockMvc.perform(post("/service/livros")
				.contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isCreated())
			.andExpect(header().doesNotExist("Location"))
			.andExpect(jsonPath("$.isbn").value("9780110002224"))
			.andExpect(jsonPath("$.categoria.nome").value("Romance"))
			.andExpect(jsonPath("$.autores.length()").value(2))
			.andExpect(jsonPath("$.categoria.livros").doesNotExist())
			.andExpect(jsonPath("$.autores[0].livros").doesNotExist())
			.andExpect(jsonPath("$.exemplares").doesNotExist())
			.andReturn().getResponse().getContentAsString();

		Long id = objectMapper.readTree(resposta).get("id").asLong();
		assertThat(livroRepository.findById(id).orElseThrow().getAutores()).hasSize(2);
		mockMvc.perform(get("/service/livros/" + id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.autores.length()").value(2));
		mockMvc.perform(get("/service/livros"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].titulo").value("Dom Casmurro"));
	}

	@Test
	public void testSaveSemCategoria() throws Exception {
		mockMvc.perform(post("/service/livros").contentType(MediaType.APPLICATION_JSON)
				.content("{\"titulo\":\"Livro\",\"isbn\":\"9780110002224\"}"))
			.andExpect(status().isBadRequest());
		assertThat(livroRepository.count()).isZero();
	}

	@Test
	public void testSaveIsbnInvalidoEDuplicado() throws Exception {
		String json = objectMapper.writeValueAsString(Map.of("titulo", "Livro",
				"isbn", "9780110002225", "categoriaId", categoria.getId()));
		mockMvc.perform(post("/service/livros").contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.mensagem").value("ISBN inválido"));

		salvar();
		json = json.replace("9780110002225", "9780110002224");
		mockMvc.perform(post("/service/livros").contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.mensagem").value("ISBN já cadastrado"));
		assertThat(livroRepository.count()).isEqualTo(1);
	}

	@Test
	public void testSaveRelacionamentoInexistente() throws Exception {
		String json = objectMapper.writeValueAsString(Map.of("titulo", "Livro",
				"isbn", "9780110002224", "categoriaId", categoria.getId(),
				"autoresIds", new Long[] {999999L}));
		mockMvc.perform(post("/service/livros").contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isNotFound());

		json = objectMapper.writeValueAsString(Map.of("titulo", "Livro",
				"isbn", "9780110002224", "categoriaId", 999999L));
		mockMvc.perform(post("/service/livros").contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isNotFound());
		assertThat(livroRepository.count()).isZero();
	}

	@Test
	public void testUpdate() throws Exception {
		Livro livro = salvar();
		Categoria outra = categoriaRepository.save(new Categoria("Literatura", null));
		Autor outro = autorRepository.save(new Autor("Autor atualizado", null, null));
		String json = objectMapper.writeValueAsString(Map.of("titulo", "Título atualizado",
				"isbn", "9780110002231", "categoriaId", outra.getId(),
				"autoresIds", new Long[] {outro.getId()}));

		mockMvc.perform(put("/service/livros/" + livro.getId())
				.contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.categoria.id").value(outra.getId()))
			.andExpect(jsonPath("$.autores[0].id").value(outro.getId()));
		Livro atualizado = livroRepository.findById(livro.getId()).orElseThrow();
		assertThat(atualizado.getTitulo()).isEqualTo("Título atualizado");
		assertThat(atualizado.getAutores()).extracting(Autor::getId).containsExactly(outro.getId());
	}

	@Test
	public void testDelete() throws Exception {
		Livro livro = salvar();
		mockMvc.perform(delete("/service/livros/" + livro.getId()))
			.andExpect(status().isNoContent());
		assertThat(livroRepository.existsById(livro.getId())).isFalse();
		assertThat(autorRepository.existsById(autor.getId())).isTrue();
		assertThat(categoriaRepository.existsById(categoria.getId())).isTrue();
	}

	@Test
	public void testFindByIdInexistente() throws Exception {
		mockMvc.perform(get("/service/livros/999999")).andExpect(status().isNotFound());
		mockMvc.perform(delete("/service/livros/999999")).andExpect(status().isNotFound());
	}

	@Test
	public void testSaveSemAutores() throws Exception {
		String json = objectMapper.writeValueAsString(Map.of("titulo", "Livro sem autor",
				"isbn", "9780110002224", "categoriaId", categoria.getId()));
		mockMvc.perform(post("/service/livros").contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.autores").isEmpty());
	}
}
