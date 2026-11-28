package br.edu.bibliotech.api;

import static org.assertj.core.api.Assertions.assertThat;
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
import br.edu.bibliotech.service.EmprestimoService;

@SpringBootTest(properties = {
	"spring.datasource.url=jdbc:h2:mem:emprestimos-web;DB_CLOSE_DELAY=-1",
	"spring.jpa.hibernate.ddl-auto=create-drop",
	"spring.jpa.open-in-view=false",
	"spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc(addFilters = false)
class EmprestimoRestTest {
	@Test
	void deveRetornar404QuandoListaEstaVazia() throws Exception {

		var resposta = mockMvc.perform(get("/service/emprestimos"));

		resposta.andExpect(status().isNotFound());
	}

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EmprestimoService emprestimoService;

	@Autowired
	private EmprestimoRepository emprestimoRepository;

	@Autowired
	private ExemplarRepository exemplarRepository;

	@Autowired
	private LivroRepository livroRepository;

	@Autowired
	private CategoriaRepository categoriaRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	private Usuario usuario;
	private Exemplar exemplar;
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
		exemplar = exemplarRepository.save(new Exemplar("TOMBO-1", livro));
		usuario = usuarioRepository.save(new Usuario("Maria", "maria@email.com", "hash-teste", Perfil.LEITOR));
	}

	private Emprestimo salvar() {
		return emprestimoService.registrar(usuario.getId(), exemplar.getId());
	}

	private void registrarEmprestimoComExemplar(String codigoTombo) {
		Exemplar outro = exemplarRepository.save(new Exemplar(codigoTombo, livro));
		emprestimoService.registrar(usuario.getId(), outro.getId());
	}

	private String dados() throws Exception {
		return objectMapper.writeValueAsString(Map.of("usuarioId", usuario.getId(), "exemplarId", exemplar.getId()));
	}

	@Test
	public void testSave() throws Exception {
		String resposta = mockMvc.perform(post("/service/emprestimos").contentType(MediaType.APPLICATION_JSON).content(dados()))
			.andExpect(status().isCreated())
			.andExpect(header().doesNotExist("Location"))
			.andExpect(jsonPath("$.dataEmprestimo").value(LocalDate.now().toString()))
			.andExpect(jsonPath("$.dataPrevista").value(LocalDate.now().plusDays(14).toString()))
			.andExpect(jsonPath("$.renovado").value(false))
			.andExpect(jsonPath("$.usuario.nome").value("Maria"))
			.andExpect(jsonPath("$.usuario.senha").doesNotExist())
			.andExpect(jsonPath("$.usuario.emprestimos").doesNotExist())
			.andExpect(jsonPath("$.exemplar.status").value("EMPRESTADO"))
			.andExpect(jsonPath("$.exemplar.livro.titulo").value("Dom Casmurro"))
			.andExpect(jsonPath("$.exemplar.emprestimos").doesNotExist())
			.andReturn().getResponse().getContentAsString();

		assertThat(resposta).doesNotContain("hash-teste");
		Long id = objectMapper.readTree(resposta).get("id").asLong();
		assertThat(emprestimoRepository.findById(id).orElseThrow().getDataDevolucao()).isNull();
		assertThat(exemplarRepository.findById(exemplar.getId()).orElseThrow().getStatus()).isEqualTo(StatusExemplar.EMPRESTADO);
		mockMvc.perform(get("/service/emprestimos/" + id))
			.andExpect(status().isOk()).andExpect(jsonPath("$.exemplar.livro.categoria.nome").value("Romance"));
		mockMvc.perform(get("/service/emprestimos"))
			.andExpect(status().isOk()).andExpect(jsonPath("$[0].usuario.id").value(usuario.getId()));
	}

	@Test
	public void testFindByUsuario() throws Exception {
		salvar();
		Usuario outro = usuarioRepository.save(new Usuario("João", "joao@email.com", "hash-teste", Perfil.LEITOR));
		Exemplar segundo = exemplarRepository.save(new Exemplar("TOMBO-2", livro));
		emprestimoService.registrar(outro.getId(), segundo.getId());
		mockMvc.perform(get("/service/emprestimos").param("usuarioId", usuario.getId().toString()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].usuario.id").value(usuario.getId()));
	}

	@ParameterizedTest
	@ValueSource(strings = {"{}", "{\"usuarioId\":0,\"exemplarId\":-1}"})
	void deveRejeitarCadastroDeEmprestimoInvalido(String json) throws Exception {

		var resposta = mockMvc.perform(post("/service/emprestimos")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));

		resposta.andExpect(status().isBadRequest());
		assertThat(emprestimoRepository.count()).isZero();
		assertThat(exemplarRepository.findById(exemplar.getId()).orElseThrow().getStatus())
				.isEqualTo(StatusExemplar.DISPONIVEL);
	}

	@Test
	public void testSaveUsuarioInativo() throws Exception {
		usuario.setAtivo(false);
		usuarioRepository.save(usuario);
		mockMvc.perform(post("/service/emprestimos").contentType(MediaType.APPLICATION_JSON).content(dados()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.mensagem").value("Usuário inativo não pode emprestar"));
		assertThat(emprestimoRepository.count()).isZero();
		assertThat(exemplarRepository.findById(exemplar.getId()).orElseThrow().getStatus()).isEqualTo(StatusExemplar.DISPONIVEL);
	}

	@Test
	public void testSaveExemplarIndisponivel() throws Exception {
		exemplar.setStatus(StatusExemplar.INDISPONIVEL);
		exemplarRepository.save(exemplar);
		mockMvc.perform(post("/service/emprestimos").contentType(MediaType.APPLICATION_JSON).content(dados()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.mensagem").value("Exemplar não está disponível para empréstimo"));
		assertThat(emprestimoRepository.count()).isZero();

		exemplar.setStatus(StatusExemplar.DISPONIVEL);
		exemplarRepository.save(exemplar);
		salvar();
		mockMvc.perform(post("/service/emprestimos").contentType(MediaType.APPLICATION_JSON).content(dados()))
			.andExpect(status().isConflict());
		assertThat(emprestimoRepository.count()).isEqualTo(1);
	}

	@Test
	public void testSaveLimiteEmprestimos() throws Exception {
		registrarEmprestimoComExemplar("TOMBO-2");
		registrarEmprestimoComExemplar("TOMBO-3");
		registrarEmprestimoComExemplar("TOMBO-4");

		mockMvc.perform(post("/service/emprestimos").contentType(MediaType.APPLICATION_JSON).content(dados()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.mensagem").value("Limite de empréstimos ativos atingido"));
		assertThat(emprestimoRepository.count()).isEqualTo(3);
		assertThat(exemplarRepository.findById(exemplar.getId()).orElseThrow().getStatus()).isEqualTo(StatusExemplar.DISPONIVEL);
	}

	@Test
	public void testRenovar() throws Exception {
		Emprestimo emprestimo = salvar();
		mockMvc.perform(put("/service/emprestimos/" + emprestimo.getId() + "/renovar"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.renovado").value(true))
			.andExpect(jsonPath("$.dataPrevista").value(emprestimo.getDataPrevista().plusDays(7).toString()));
		mockMvc.perform(put("/service/emprestimos/" + emprestimo.getId() + "/renovar"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.mensagem").value("Empréstimo já foi renovado uma vez"));
		assertThat(emprestimoRepository.findById(emprestimo.getId()).orElseThrow().getDataPrevista())
			.isEqualTo(emprestimo.getDataPrevista().plusDays(7));
	}

	@Test
	public void testDevolver() throws Exception {
		Emprestimo emprestimo = emprestimoService.registrar(usuario.getId(), exemplar.getId(), LocalDate.now().minusDays(20));
		mockMvc.perform(put("/service/emprestimos/" + emprestimo.getId() + "/devolver"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.dataDevolucao").value(LocalDate.now().toString()))
			.andExpect(jsonPath("$.multa").value(6.00))
			.andExpect(jsonPath("$.exemplar.status").value("DISPONIVEL"));
		assertThat(exemplarRepository.findById(exemplar.getId()).orElseThrow().getStatus()).isEqualTo(StatusExemplar.DISPONIVEL);
		assertThat(emprestimoRepository.findById(emprestimo.getId()).orElseThrow().getMulta()).isEqualByComparingTo("6.00");
		mockMvc.perform(put("/service/emprestimos/" + emprestimo.getId() + "/devolver")).andExpect(status().isConflict());
		mockMvc.perform(put("/service/emprestimos/" + emprestimo.getId() + "/renovar")).andExpect(status().isConflict());

		mockMvc.perform(post("/service/emprestimos").contentType(MediaType.APPLICATION_JSON).content(dados()))
			.andExpect(status().isCreated());
		assertThat(emprestimoRepository.count()).isEqualTo(2);
	}

	@Test
	public void testFindByIdInexistente() throws Exception {
		mockMvc.perform(get("/service/emprestimos/999999")).andExpect(status().isNotFound());
		mockMvc.perform(get("/service/emprestimos").param("usuarioId", "999999")).andExpect(status().isNotFound());
		mockMvc.perform(put("/service/emprestimos/999999/renovar")).andExpect(status().isNotFound());
		mockMvc.perform(put("/service/emprestimos/999999/devolver")).andExpect(status().isNotFound());

		String json = objectMapper.writeValueAsString(Map.of("usuarioId", 999999L, "exemplarId", exemplar.getId()));
		mockMvc.perform(post("/service/emprestimos").contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isNotFound());
		json = objectMapper.writeValueAsString(Map.of("usuarioId", usuario.getId(), "exemplarId", 999999L));
		mockMvc.perform(post("/service/emprestimos").contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isNotFound());
		assertThat(emprestimoRepository.count()).isZero();
	}
}
