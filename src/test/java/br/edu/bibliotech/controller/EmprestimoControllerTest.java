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
import br.edu.bibliotech.service.EmprestimoService;

@SpringBootTest(properties = {
	"spring.datasource.url=jdbc:h2:mem:emprestimos-web;DB_CLOSE_DELAY=-1",
	"spring.jpa.hibernate.ddl-auto=create-drop",
	"spring.jpa.open-in-view=false",
	"spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc(addFilters = false)
class EmprestimoControllerTest {

	@Test
	void deveAbrirConfirmacaoDeDevolucao() throws Exception {
		Emprestimo emprestimo = salvar();

		var resposta = mockMvc.perform(get("/emprestimo/" + emprestimo.getId() + "/devolucao"));

		resposta.andExpect(status().isOk())
				.andExpect(view().name("emprestimo/devolucao"))
				.andExpect(content().string(containsString("Confirmar devolução")));
	}

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

	@Test
	public void deveAbrirFormularioDeEmprestimo() throws Exception {
		Usuario inativo = new Usuario("Inativo", "inativo@email.com", "hash-teste", Perfil.LEITOR);
		inativo.setAtivo(false);
		usuarioRepository.save(inativo);
		Exemplar indisponivel = new Exemplar("TOMBO-INDISPONIVEL", livro);
		indisponivel.setStatus(StatusExemplar.INDISPONIVEL);
		exemplarRepository.save(indisponivel);

		mockMvc.perform(get("/emprestimo/formulario"))
			.andExpect(status().isOk())
			.andExpect(view().name("emprestimo/formulario"))
			.andExpect(content().string(containsString("action=\"/emprestimo/novo\"")))
			.andExpect(content().string(containsString("maria@email.com")))
			.andExpect(content().string(containsString("TOMBO-1")))
			.andExpect(content().string(not(containsString("inativo@email.com"))))
			.andExpect(content().string(not(containsString("TOMBO-INDISPONIVEL"))))
			.andExpect(content().string(not(containsString("hash-teste"))));
	}

	@Test
	public void deveDesabilitarFormularioSemOpcoes() throws Exception {
		usuario.setAtivo(false);
		usuarioRepository.save(usuario);
		exemplar.setStatus(StatusExemplar.INDISPONIVEL);
		exemplarRepository.save(exemplar);
		mockMvc.perform(get("/emprestimo/formulario"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Nenhum usuário ativo disponível")))
			.andExpect(content().string(containsString("Nenhum exemplar disponível")))
			.andExpect(content().string(containsString("<button type=\"submit\" disabled=\"disabled\">Salvar</button>")));
	}

	@Test
	public void deveRegistrarEmprestimoValido() throws Exception {
		mockMvc.perform(post("/emprestimo/novo").param("usuarioId", usuario.getId().toString())
				.param("exemplarId", exemplar.getId().toString()))
			.andExpect(redirectedUrl("/home"));
		Emprestimo emprestimo = emprestimoRepository.findAll().get(0);
		assertThat(emprestimo.getUsuario().getId()).isEqualTo(usuario.getId());
		assertThat(emprestimo.getDataPrevista()).isEqualTo(LocalDate.now().plusDays(14));
		assertThat(exemplarRepository.findById(exemplar.getId()).orElseThrow().getStatus()).isEqualTo(StatusExemplar.EMPRESTADO);
		mockMvc.perform(get("/emprestimo"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Maria")))
				.andExpect(content().string(containsString("TOMBO-1")))
				.andExpect(content().string(containsString("Em aberto")))
				.andExpect(content().string(containsString("/emprestimo/" + emprestimo.getId() + "/devolucao")))
				.andExpect(content().string(not(containsString("hash-teste"))));
	}

	@Test
	public void deveFiltrarEmprestimosPorUsuario() throws Exception {
		salvar();
		Usuario outro = usuarioRepository.save(new Usuario("João", "joao@email.com", "hash-teste", Perfil.LEITOR));
		Exemplar segundo = exemplarRepository.save(new Exemplar("TOMBO-2", livro));
		emprestimoService.registrar(outro.getId(), segundo.getId());
		mockMvc.perform(get("/emprestimo").param("usuarioId", usuario.getId().toString()))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("TOMBO-1")))
			.andExpect(content().string(not(containsString("TOMBO-2"))));
	}

	@Test
	public void deveReexibirFormularioQuandoEmprestimoInvalido() throws Exception {
		mockMvc.perform(post("/emprestimo/novo").param("usuarioId", "abc").param("exemplarId", ""))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasFieldErrors("requisicaoNovoEmprestimo", "usuarioId", "exemplarId"))
			.andExpect(content().string(containsString("Selecione um usuário válido")))
			.andExpect(content().string(containsString("TOMBO-1")));

		mockMvc.perform(post("/emprestimo/novo").param("usuarioId", usuario.getId().toString()).param("exemplarId", "999999"))
			.andExpect(view().name("emprestimo/formulario"))
			.andExpect(model().attributeHasErrors("requisicaoNovoEmprestimo"))
			.andExpect(content().string(containsString("Exemplar não encontrado")))
			.andExpect(content().string(containsString("value=\"" + usuario.getId() + "\" selected=\"selected\"")));
		assertThat(emprestimoRepository.count()).isZero();
	}

	@Test
	public void deveRejeitarExemplarEmprestado() throws Exception {
		salvar();
		mockMvc.perform(post("/emprestimo/novo").param("usuarioId", usuario.getId().toString())
				.param("exemplarId", exemplar.getId().toString()))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasErrors("requisicaoNovoEmprestimo"))
			.andExpect(content().string(containsString("Exemplar não está disponível para empréstimo")));
		assertThat(emprestimoRepository.count()).isEqualTo(1);
	}

	@Test
	public void deveRenovarEmprestimoUmaVez() throws Exception {
		Emprestimo emprestimo = salvar();
		mockMvc.perform(post("/emprestimo/" + emprestimo.getId() + "/renovar")).andExpect(redirectedUrl("/home"));
		assertThat(emprestimoRepository.findById(emprestimo.getId()).orElseThrow().getDataPrevista())
			.isEqualTo(emprestimo.getDataPrevista().plusDays(7));
		mockMvc.perform(get("/emprestimo"))
			.andExpect(status().isOk())
			.andExpect(content().string(not(containsString("/emprestimo/" + emprestimo.getId() + "/renovar"))));
		String mensagem = "Empréstimo já foi renovado uma vez";
		mockMvc.perform(post("/emprestimo/" + emprestimo.getId() + "/renovar"))
			.andExpect(redirectedUrl("/emprestimo"))
			.andExpect(flash().attribute("erro", mensagem));
		mockMvc.perform(get("/emprestimo").flashAttr("erro", mensagem))
			.andExpect(content().string(containsString(mensagem)));
	}

	@Test
	public void deveDevolverEmprestimoECalcularMulta() throws Exception {
		Emprestimo emprestimo = emprestimoService.registrar(usuario.getId(), exemplar.getId(), LocalDate.now().minusDays(20));
		mockMvc.perform(post("/emprestimo/" + emprestimo.getId() + "/devolver")).andExpect(redirectedUrl("/home"));
		Emprestimo devolvido = emprestimoRepository.findById(emprestimo.getId()).orElseThrow();
		assertThat(devolvido.getDataDevolucao()).isEqualTo(LocalDate.now());
		assertThat(devolvido.getMulta()).isEqualByComparingTo("6.00");
		assertThat(exemplarRepository.findById(exemplar.getId()).orElseThrow().getStatus()).isEqualTo(StatusExemplar.DISPONIVEL);
		mockMvc.perform(get("/emprestimo"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Devolvido")))
			.andExpect(content().string(containsString("R$ 6,00")))
			.andExpect(content().string(not(containsString("/emprestimo/" + emprestimo.getId() + "/devolver"))))
			.andExpect(content().string(not(containsString("/emprestimo/" + emprestimo.getId() + "/renovar"))));
		mockMvc.perform(post("/emprestimo/" + emprestimo.getId() + "/devolver"))
			.andExpect(redirectedUrl("/emprestimo"))
			.andExpect(flash().attribute("erro", "Empréstimo já devolvido"));
	}

	@Test
	public void deveRetornar404QuandoEmprestimoNaoExiste() throws Exception {
		mockMvc.perform(get("/emprestimo/999999/devolucao")).andExpect(status().isNotFound());
		mockMvc.perform(post("/emprestimo/999999/renovar")).andExpect(status().isNotFound()).andExpect(view().name("erro"));
		mockMvc.perform(post("/emprestimo/999999/devolver")).andExpect(status().isNotFound()).andExpect(view().name("erro"));
		mockMvc.perform(get("/emprestimo").param("usuarioId", "999999")).andExpect(status().isNotFound());
	}
}
