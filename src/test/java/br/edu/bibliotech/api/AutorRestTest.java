package br.edu.bibliotech.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import br.edu.bibliotech.model.Autor;
import br.edu.bibliotech.service.AutorService;

import jakarta.persistence.EntityNotFoundException;

@WebMvcTest(AutorRest.class)
@AutoConfigureMockMvc(addFilters = false)
class AutorRestTest {
	@Test
	void deveRetornar404QuandoListaEstaVazia() throws Exception {
		when(autorService.listar()).thenReturn(List.of());

		var resposta = mockMvc.perform(get("/service/autores"));

		resposta.andExpect(status().isNotFound());
	}

	@Test
	void deveBuscarAutorExistentePorId() throws Exception {
		Autor autor = new Autor("Machado de Assis", "Brasileira", null);
		autor.setId(10L);
		when(autorService.buscarPorId(10L)).thenReturn(autor);

		var resposta = mockMvc.perform(get("/service/autores/10"));

		resposta.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Machado de Assis"));
	}

	@Test
	void deveRetornar404AoAtualizarAutorInexistente() throws Exception {
		when(autorService.atualizar(org.mockito.ArgumentMatchers.eq(99L), any(Autor.class)))
				.thenThrow(new EntityNotFoundException("Autor não encontrado"));

		var resposta = mockMvc.perform(put("/service/autores/99")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Machado de Assis\"}"));

		resposta.andExpect(status().isNotFound());
	}

	@Test
	void deveAtualizarAutorExistente() throws Exception {
		Autor autor = new Autor("Machado atualizado", "Brasileira", null);
		autor.setId(10L);
		when(autorService.atualizar(org.mockito.ArgumentMatchers.eq(10L), any(Autor.class)))
				.thenReturn(autor);

		var resposta = mockMvc.perform(put("/service/autores/10")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Machado atualizado\"}"));

		resposta.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Machado atualizado"));
	}

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private AutorService autorService;

	@Test
	public void testSave() throws Exception {

		Autor autor = new Autor();
		autor.setId(10L);
		autor.setNome("Machado de Assis");
		autor.setNacionalidade("Brasileira");
		autor.setDataNascimento(LocalDate.of(1839, 6, 21));

		when(autorService.cadastrar(any(Autor.class))).thenReturn(autor);

		mockMvc.perform(post("/service/autores")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "nome": "Machado de Assis",
					  "nacionalidade": "Brasileira",
					  "dataNascimento": "1839-06-21"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(header().doesNotExist("Location"))
			.andExpect(jsonPath("$.id").value(10))
			.andExpect(jsonPath("$.nome").value("Machado de Assis"))
			.andExpect(jsonPath("$.dataNascimento").value("1839-06-21"))
			.andExpect(jsonPath("$.livros").doesNotExist());
	}

	@Test
	public void testSaveSemNome() throws Exception {

		mockMvc.perform(post("/service/autores")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"nome": "", "nacionalidade": "Brasileira"}
					"""))
			.andExpect(status().isBadRequest());

		verify(autorService, never()).cadastrar(any(Autor.class));
	}

	@Test
	public void testFindAll() throws Exception {

		Autor autor = new Autor();
		autor.setId(20L);
		autor.setNome("Clarice Lispector");
		autor.setNacionalidade("Brasileira");

		when(autorService.listar()).thenReturn(List.of(autor));

		mockMvc.perform(get("/service/autores"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(20))
			.andExpect(jsonPath("$[0].nome").value("Clarice Lispector"));
	}

	@Test
	public void testFindByIdInexistente() throws Exception {

		when(autorService.buscarPorId(999L))
				.thenThrow(new EntityNotFoundException("Autor não encontrado: 999"));

		mockMvc.perform(get("/service/autores/999"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.mensagem").value("Autor não encontrado: 999"));
	}

	@Test
	public void testDelete() throws Exception {

		mockMvc.perform(delete("/service/autores/10"))
			.andExpect(status().isNoContent());

		verify(autorService).excluir(10L);
	}
}
