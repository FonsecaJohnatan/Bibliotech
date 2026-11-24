package br.edu.bibliotech.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.edu.bibliotech.model.Autor;
import br.edu.bibliotech.model.Categoria;
import br.edu.bibliotech.repository.AutorRepository;
import br.edu.bibliotech.repository.CategoriaRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PaginasTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AutorRepository autorRepository;

	@Autowired
	private CategoriaRepository categoriaRepository;

	@Test
	public void testHome() throws Exception {
		mockMvc.perform(get("/"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Bem-vindo ao BiblioTech")))
			.andExpect(content().string(containsString("href=\"/autor\"")))
			.andExpect(content().string(containsString("href=\"/categoria\"")));
	}

	@ParameterizedTest
	@CsvSource({"autores, autor", "categorias, categoria"})
	public void testFormulario(String rota, String pasta) throws Exception {
		mockMvc.perform(get("/" + pasta + "/formulario"))
			.andExpect(status().isOk())
			.andExpect(view().name(pasta + "/formulario"))
			.andExpect(content().string(containsString("action=\"/" + pasta + "/novo\"")))
			.andExpect(content().string(containsString("name=\"nome\"")));
	}

	@ParameterizedTest
	@CsvSource({"autores, autor", "categorias, categoria"})
	public void testSave(String rota, String pasta) throws Exception {
		mockMvc.perform(post("/" + pasta + "/novo").param("nome", "Cadastro MVC"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/home"));

		mockMvc.perform(get("/" + pasta))
			.andExpect(status().isOk())
			.andExpect(view().name(pasta + "/lista"))
			.andExpect(content().string(containsString("Cadastro MVC")));

		mockMvc.perform(get("/service/" + rota))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(content().string(containsString("Cadastro MVC")));
	}

	@ParameterizedTest
	@CsvSource({"autores, autor", "categorias, categoria"})
	public void testSaveSemNome(String rota, String pasta) throws Exception {
		mockMvc.perform(post("/" + pasta + "/novo").param("nome", " "))
			.andExpect(status().isOk())
			.andExpect(view().name(pasta + "/formulario"))
			.andExpect(model().attributeHasFieldErrors(nomeDto(pasta), "nome"))
			.andExpect(content().string(containsString("obrigatório")));
	}

	@ParameterizedTest
	@CsvSource({"autores, autor", "categorias, categoria"})
	public void testUpdate(String rota, String pasta) throws Exception {
		Long id = salvar(rota);

		mockMvc.perform(get("/" + pasta + "/" + id + "/editar"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("value=\"Nome original\"")))
			.andExpect(content().string(containsString("action=\"/" + pasta + "/" + id + "\"")));

		mockMvc.perform(post("/" + pasta + "/" + id).param("nome", ""))
			.andExpect(view().name(pasta + "/formulario"))
			.andExpect(model().attributeHasFieldErrors(nomeDto(pasta), "nome"))
			.andExpect(content().string(containsString("action=\"/" + pasta + "/" + id + "\"")));

		mockMvc.perform(post("/" + pasta + "/" + id).param("nome", "Nome atualizado"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/home"));

		mockMvc.perform(get("/service/" + rota + "/" + id))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Nome atualizado")));
	}

	@ParameterizedTest
	@CsvSource({"autores, autor", "categorias, categoria"})
	public void testFindByIdInexistente(String rota, String pasta) throws Exception {
		mockMvc.perform(get("/" + pasta + "/999999/editar"))
			.andExpect(status().isNotFound())
			.andExpect(view().name("erro"))
			.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));

		mockMvc.perform(post("/" + pasta + "/999999").param("nome", "Teste"))
			.andExpect(status().isNotFound())
			.andExpect(view().name("erro"));

		mockMvc.perform(get("/service/" + rota + "/999999"))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
	}

	@Test
	public void testCategoriaDuplicada() throws Exception {
		Long id = salvar("categorias");
		categoriaRepository.save(new Categoria("Outra categoria", null));

		mockMvc.perform(post("/categoria/novo").param("nome", "Nome original"))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasErrors("requisicaoNovaCategoria"))
			.andExpect(content().string(containsString("Nome de categoria já cadastrado")));

		mockMvc.perform(post("/categoria/" + id).param("nome", "Outra categoria"))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasErrors("requisicaoNovaCategoria"))
			.andExpect(content().string(containsString("Nome de categoria já cadastrado")));
	}

	@Test
	public void testDataNascimento() throws Exception {
		mockMvc.perform(post("/autor/novo").param("nome", "Machado de Assis")
				.param("dataNascimento", "1839-06-21"))
			.andExpect(redirectedUrl("/home"));

		Autor autor = autorRepository.findByNomeContainingIgnoreCase("Machado").get(0);
		mockMvc.perform(get("/autor/" + autor.getId() + "/editar"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("value=\"1839-06-21\"")));

		mockMvc.perform(post("/autor/novo").param("nome", "Outro autor")
				.param("dataNascimento", "data-invalida"))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasFieldErrors("requisicaoNovoAutor", "dataNascimento"))
			.andExpect(content().string(containsString("Informe uma data válida")));
	}

	@Test
	public void testNomeComHtml() throws Exception {
		autorRepository.save(new Autor("<script>alert(1)</script>", null, null));

		mockMvc.perform(get("/autor"))
			.andExpect(status().isOk())
			.andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
			.andExpect(content().string(containsString("&lt;script&gt;")));
	}

	private String nomeDto(String pasta) {
		return pasta.equals("autor") ? "requisicaoNovoAutor" : "requisicaoNovaCategoria";
	}

	private Long salvar(String rota) {
		if (rota.equals("autores")) {
			return autorRepository.save(new Autor("Nome original", null, null)).getId();
		}
		return categoriaRepository.save(new Categoria("Nome original", null)).getId();
	}
}
