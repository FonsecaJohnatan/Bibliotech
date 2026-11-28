package br.edu.bibliotech.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.bibliotech.model.Categoria;
import br.edu.bibliotech.model.Emprestimo;
import br.edu.bibliotech.model.Exemplar;
import br.edu.bibliotech.model.Livro;
import br.edu.bibliotech.model.Perfil;
import br.edu.bibliotech.model.StatusExemplar;
import br.edu.bibliotech.model.Usuario;
import br.edu.bibliotech.repository.CategoriaRepository;
import br.edu.bibliotech.repository.EmprestimoRepository;
import br.edu.bibliotech.repository.ExemplarRepository;
import br.edu.bibliotech.repository.LivroRepository;
import br.edu.bibliotech.repository.UsuarioRepository;

@SpringBootTest(properties = {
	"spring.datasource.url=jdbc:h2:mem:exemplares-web;DB_CLOSE_DELAY=-1",
	"spring.jpa.hibernate.ddl-auto=create-drop",
	"spring.jpa.open-in-view=false",
	"spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc(addFilters = false)
class ExemplarRestTest {
	@Test
	void deveRetornar404QuandoListaEstaVazia() throws Exception {

		var resposta = mockMvc.perform(get("/service/exemplares"));

		resposta.andExpect(status().isNotFound());
	}

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ExemplarRepository exemplarRepository;

	@Autowired
	private LivroRepository livroRepository;

	@Autowired
	private CategoriaRepository categoriaRepository;

	@Autowired
	private EmprestimoRepository emprestimoRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	private Livro livro;

	@BeforeEach
	public void preparar() {
		emprestimoRepository.deleteAll();
		exemplarRepository.deleteAll();
		livroRepository.deleteAll();
		categoriaRepository.deleteAll();
		usuarioRepository.deleteAll();
		Categoria categoria = categoriaRepository.save(new Categoria("Romance", null));
		livro = livroRepository.save(new Livro("Dom Casmurro", "9780110002224", 1899, categoria));
	}

	private Exemplar salvar() {
		return exemplarRepository.save(new Exemplar("TOMBO-1", livro));
	}

	private void salvarHistorico(Exemplar exemplar) {
		Usuario usuario = usuarioRepository.save(new Usuario("Maria", "maria@email.com", "hash-teste", Perfil.LEITOR));
		Emprestimo emprestimo = new Emprestimo(exemplar, usuario, LocalDate.now().minusDays(7));
		emprestimo.setDataDevolucao(LocalDate.now());
		emprestimoRepository.save(emprestimo);
	}

	@Test
	public void testSave() throws Exception {
		String json = objectMapper.writeValueAsString(Map.of("codigoTombo", " tombo-1 ",
				"livroId", livro.getId(), "status", "EMPRESTADO"));
		String resposta = mockMvc.perform(post("/service/exemplares").contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isCreated())
			.andExpect(header().doesNotExist("Location"))
			.andExpect(jsonPath("$.codigoTombo").value("TOMBO-1"))
			.andExpect(jsonPath("$.status").value("DISPONIVEL"))
			.andExpect(jsonPath("$.livro.titulo").value("Dom Casmurro"))
			.andExpect(jsonPath("$.livro.categoria.nome").value("Romance"))
			.andExpect(jsonPath("$.livro.autores").isArray())
			.andExpect(jsonPath("$.livro.exemplares").doesNotExist())
			.andExpect(jsonPath("$.emprestimos").doesNotExist())
			.andReturn().getResponse().getContentAsString();

		Long id = objectMapper.readTree(resposta).get("id").asLong();
		assertThat(exemplarRepository.findById(id).orElseThrow().getCodigoTombo()).isEqualTo("TOMBO-1");
		mockMvc.perform(get("/service/exemplares/" + id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.livro.titulo").value("Dom Casmurro"));
		mockMvc.perform(get("/service/exemplares"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].codigoTombo").value("TOMBO-1"));
	}

	@Test
	public void testFindByLivro() throws Exception {
		salvar();
		Livro outro = livroRepository.save(new Livro("Outro livro", "9780110002231", 2020, livro.getCategoria()));
		exemplarRepository.save(new Exemplar("TOMBO-2", outro));
		mockMvc.perform(get("/service/exemplares").param("livroId", livro.getId().toString()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].livro.id").value(livro.getId()));
		exemplarRepository.deleteAll();
		mockMvc.perform(get("/service/exemplares").param("livroId", livro.getId().toString()))
			.andExpect(status().isNotFound());
	}

	@ParameterizedTest
	@ValueSource(strings = {"{}", "{\"codigoTombo\":\"TOMBO-1\",\"livroId\":0}"})
	void deveRejeitarCadastroDeExemplarInvalido(String json) throws Exception {

		var resposta = mockMvc.perform(post("/service/exemplares")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));

		resposta.andExpect(status().isBadRequest());
		assertThat(exemplarRepository.count()).isZero();
	}

	@Test
	void deveRejeitarCodigoDeTomboLongo() throws Exception {
		String json = objectMapper.writeValueAsString(Map.of(
				"codigoTombo", "AAAAAAAAAAAAAAAAAAAAA",
				"livroId", livro.getId()));

		var resposta = mockMvc.perform(post("/service/exemplares")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));

		resposta.andExpect(status().isBadRequest());
		assertThat(exemplarRepository.count()).isZero();
	}

	@Test
	public void testSaveCodigoDuplicado() throws Exception {
		salvar();
		String json = objectMapper.writeValueAsString(Map.of("codigoTombo", " tombo-1 ", "livroId", livro.getId()));
		mockMvc.perform(post("/service/exemplares").contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.mensagem").value("Código de tombo já cadastrado"));
		assertThat(exemplarRepository.count()).isEqualTo(1);
	}

	@Test
	public void testUpdate() throws Exception {
		Exemplar exemplar = salvar();
		exemplar.setStatus(StatusExemplar.INDISPONIVEL);
		exemplarRepository.save(exemplar);
		Livro outro = livroRepository.save(new Livro("Outro livro", "9780110002231", 2020, livro.getCategoria()));
		String json = objectMapper.writeValueAsString(Map.of("codigoTombo", "tombo-2",
				"livroId", outro.getId(), "status", "DISPONIVEL"));
		mockMvc.perform(put("/service/exemplares/" + exemplar.getId()).contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.codigoTombo").value("TOMBO-2"))
			.andExpect(jsonPath("$.status").value("INDISPONIVEL"))
			.andExpect(jsonPath("$.livro.id").value(outro.getId()));
		assertThat(exemplarRepository.findById(exemplar.getId()).orElseThrow().getLivro().getId()).isEqualTo(outro.getId());
	}

	@Test
	public void testUpdateComHistorico() throws Exception {
		Exemplar exemplar = salvar();
		salvarHistorico(exemplar);
		Livro outro = livroRepository.save(new Livro("Outro livro", "9780110002231", 2020, livro.getCategoria()));
		String json = objectMapper.writeValueAsString(Map.of("codigoTombo", "TOMBO-2", "livroId", outro.getId()));
		mockMvc.perform(put("/service/exemplares/" + exemplar.getId()).contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.mensagem").value("Exemplar com histórico não pode mudar de livro"));
		Exemplar salvo = exemplarRepository.findById(exemplar.getId()).orElseThrow();
		assertThat(salvo.getLivro().getId()).isEqualTo(livro.getId());
		assertThat(salvo.getCodigoTombo()).isEqualTo("TOMBO-1");

		json = objectMapper.writeValueAsString(Map.of("codigoTombo", "TOMBO-2", "livroId", livro.getId()));
		mockMvc.perform(put("/service/exemplares/" + exemplar.getId()).contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isOk()).andExpect(jsonPath("$.codigoTombo").value("TOMBO-2"));
	}

	@Test
	public void testDelete() throws Exception {
		Exemplar exemplar = salvar();
		mockMvc.perform(delete("/service/exemplares/" + exemplar.getId())).andExpect(status().isNoContent());
		assertThat(exemplarRepository.existsById(exemplar.getId())).isFalse();
		assertThat(livroRepository.existsById(livro.getId())).isTrue();
	}

	@Test
	public void testDeleteComHistoricoOuEmprestado() throws Exception {
		Exemplar exemplar = salvar();
		salvarHistorico(exemplar);
		mockMvc.perform(delete("/service/exemplares/" + exemplar.getId()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.mensagem").value("Exemplar emprestado ou com histórico não pode ser excluído"));
		assertThat(emprestimoRepository.count()).isEqualTo(1);
		assertThat(exemplarRepository.existsById(exemplar.getId())).isTrue();

		emprestimoRepository.deleteAll();
		exemplar.setStatus(StatusExemplar.EMPRESTADO);
		exemplarRepository.save(exemplar);
		mockMvc.perform(delete("/service/exemplares/" + exemplar.getId())).andExpect(status().isConflict());
		assertThat(exemplarRepository.existsById(exemplar.getId())).isTrue();
	}

	@Test
	public void testFindByIdInexistente() throws Exception {
		mockMvc.perform(get("/service/exemplares/999999")).andExpect(status().isNotFound());
		mockMvc.perform(delete("/service/exemplares/999999")).andExpect(status().isNotFound());
		mockMvc.perform(get("/service/exemplares").param("livroId", "999999")).andExpect(status().isNotFound());

		String json = objectMapper.writeValueAsString(Map.of("codigoTombo", "TOMBO-1", "livroId", 999999L));
		mockMvc.perform(post("/service/exemplares").contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isNotFound());
		assertThat(exemplarRepository.count()).isZero();

		json = objectMapper.writeValueAsString(Map.of("codigoTombo", "TOMBO-1", "livroId", livro.getId()));
		mockMvc.perform(put("/service/exemplares/999999").contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isNotFound());
	}
}
