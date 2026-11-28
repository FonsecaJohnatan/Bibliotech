package br.edu.bibliotech.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

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
class ExemplarControllerTest {

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
	public void deveAbrirFormularioDeExemplar() throws Exception {
		mockMvc.perform(get("/exemplar/formulario"))
			.andExpect(status().isOk())
			.andExpect(view().name("exemplar/formulario"))
			.andExpect(content().string(containsString("action=\"/exemplar/novo\"")))
			.andExpect(content().string(containsString("Dom Casmurro")));
		livroRepository.deleteAll();
		mockMvc.perform(get("/exemplar/formulario"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("antes de salvar o exemplar")))
			.andExpect(content().string(containsString("<button type=\"submit\" disabled=\"disabled\">Salvar</button>")));
	}

	@Test
	public void deveCadastrarExemplarValido() throws Exception {
		mockMvc.perform(post("/exemplar/novo").param("codigoTombo", " tombo-1 ").param("livroId", livro.getId().toString()))
			.andExpect(redirectedUrl("/home"));
		assertThat(exemplarRepository.findAll().get(0).getStatus()).isEqualTo(StatusExemplar.DISPONIVEL);
		mockMvc.perform(get("/exemplar"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("TOMBO-1")))
				.andExpect(content().string(containsString("Disponível")));
	}

	@Test
	public void deveFiltrarExemplaresPorLivro() throws Exception {
		salvar();
		Livro outro = livroRepository.save(new Livro("Outro livro", "9780110002231", 2020, livro.getCategoria()));
		exemplarRepository.save(new Exemplar("TOMBO-2", outro));
		mockMvc.perform(get("/exemplar").param("livroId", livro.getId().toString()))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("TOMBO-1")))
			.andExpect(content().string(not(containsString("TOMBO-2"))));
	}

	@Test
	public void deveReexibirFormularioQuandoExemplarInvalido() throws Exception {
		mockMvc.perform(post("/exemplar/novo").param("codigoTombo", "").param("livroId", "abc"))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasFieldErrors("requisicaoNovoExemplar", "codigoTombo", "livroId"))
			.andExpect(content().string(containsString("Selecione um livro válido")));
		mockMvc.perform(post("/exemplar/novo").param("codigoTombo", "TOMBO-1").param("livroId", "999999"))
			.andExpect(view().name("exemplar/formulario"))
			.andExpect(content().string(containsString("Livro não encontrado")));
		assertThat(exemplarRepository.count()).isZero();
		salvar();
		mockMvc.perform(post("/exemplar/novo").param("codigoTombo", "tombo-1").param("livroId", livro.getId().toString()))
			.andExpect(model().attributeHasErrors("requisicaoNovoExemplar"))
			.andExpect(content().string(containsString("Código de tombo já cadastrado")))
			.andExpect(content().string(containsString("value=\"" + livro.getId() + "\" selected=\"selected\"")));
		assertThat(exemplarRepository.count()).isEqualTo(1);
	}

	@Test
	public void deveAtualizarExemplar() throws Exception {
		Exemplar exemplar = salvar();
		mockMvc.perform(get("/exemplar/" + exemplar.getId() + "/editar"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("value=\"TOMBO-1\"")))
			.andExpect(content().string(containsString("value=\"" + livro.getId() + "\" selected=\"selected\"")));
		mockMvc.perform(post("/exemplar/" + exemplar.getId()).param("codigoTombo", "")
				.param("livroId", livro.getId().toString()))
			.andExpect(view().name("exemplar/formulario"))
			.andExpect(model().attributeHasFieldErrors("requisicaoNovoExemplar", "codigoTombo"))
			.andExpect(content().string(containsString("action=\"/exemplar/" + exemplar.getId() + "\"")));
		mockMvc.perform(post("/exemplar/" + exemplar.getId()).param("codigoTombo", "tombo-2")
				.param("livroId", livro.getId().toString()))
			.andExpect(redirectedUrl("/home"));
		assertThat(exemplarRepository.findById(exemplar.getId()).orElseThrow().getCodigoTombo()).isEqualTo("TOMBO-2");
	}

	@Test
	public void deveRejeitarMudancaDeLivroComHistorico() throws Exception {
		Exemplar exemplar = salvar();
		salvarHistorico(exemplar);
		Livro outro = livroRepository.save(new Livro("Outro livro", "9780110002231", 2020, livro.getCategoria()));
		mockMvc.perform(post("/exemplar/" + exemplar.getId()).param("codigoTombo", "TOMBO-2")
				.param("livroId", outro.getId().toString()))
			.andExpect(view().name("exemplar/formulario"))
			.andExpect(model().attributeHasErrors("requisicaoNovoExemplar"))
			.andExpect(content().string(containsString("Exemplar com histórico não pode mudar de livro")));
		assertThat(exemplarRepository.findById(exemplar.getId()).orElseThrow().getLivro().getId()).isEqualTo(livro.getId());
	}

	@Test
	public void deveExcluirExemplarSemHistorico() throws Exception {
		Exemplar exemplar = salvar();
		mockMvc.perform(post("/exemplar/" + exemplar.getId() + "/excluir"))
			.andExpect(redirectedUrl("/home"));
		assertThat(exemplarRepository.existsById(exemplar.getId())).isFalse();
	}

	@Test
	public void deveRejeitarExclusaoDeExemplarComHistorico() throws Exception {
		Exemplar exemplar = salvar();
		salvarHistorico(exemplar);
		String mensagem = "Exemplar emprestado ou com histórico não pode ser excluído";
		mockMvc.perform(post("/exemplar/" + exemplar.getId() + "/excluir"))
			.andExpect(redirectedUrl("/exemplar"))
			.andExpect(flash().attribute("erro", mensagem));
		mockMvc.perform(get("/exemplar").flashAttr("erro", mensagem))
			.andExpect(status().isOk()).andExpect(content().string(containsString(mensagem)));
		assertThat(exemplarRepository.existsById(exemplar.getId())).isTrue();
	}

	@Test
	public void deveRetornar404QuandoExemplarNaoExiste() throws Exception {
		mockMvc.perform(get("/exemplar/999999/editar")).andExpect(status().isNotFound()).andExpect(view().name("erro"));
		mockMvc.perform(post("/exemplar/999999").param("codigoTombo", "TOMBO-1").param("livroId", livro.getId().toString()))
			.andExpect(status().isNotFound()).andExpect(view().name("erro"));
		mockMvc.perform(post("/exemplar/999999/excluir")).andExpect(status().isNotFound());
		mockMvc.perform(get("/exemplar").param("livroId", "999999")).andExpect(status().isNotFound());
	}
}
