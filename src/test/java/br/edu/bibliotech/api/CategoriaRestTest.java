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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import br.edu.bibliotech.model.Categoria;
import br.edu.bibliotech.service.CategoriaService;

@WebMvcTest(CategoriaRest.class)
class CategoriaRestTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private CategoriaService categoriaService;

	@Test
	public void testSave() throws Exception {

		Categoria categoria = new Categoria();
		categoria.setId(10L);
		categoria.setNome("Romance");
		categoria.setDescricao("Livros de ficção");

		when(categoriaService.cadastrar(any(Categoria.class))).thenReturn(categoria);

		mockMvc.perform(post("/service/categorias")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"nome": "Romance", "descricao": "Livros de ficção"}
					"""))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "/service/categorias/10"))
			.andExpect(jsonPath("$.id").value(10))
			.andExpect(jsonPath("$.nome").value("Romance"))
			.andExpect(jsonPath("$.livros").doesNotExist());
	}

	@Test
	public void testSaveDescricaoAcimaDoLimite() throws Exception {

		String descricao = "D".repeat(256);

		mockMvc.perform(post("/service/categorias")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Romance\",\"descricao\":\"" + descricao + "\"}"))
			.andExpect(status().isBadRequest());

		verify(categoriaService, never()).cadastrar(any(Categoria.class));
	}

	@Test
	public void testFindAll() throws Exception {

		Categoria categoria = new Categoria();
		categoria.setId(20L);
		categoria.setNome("Fantasia");

		when(categoriaService.listar()).thenReturn(List.of(categoria));

		mockMvc.perform(get("/service/categorias"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(20))
			.andExpect(jsonPath("$[0].nome").value("Fantasia"));
	}

	@Test
	public void testUpdate() throws Exception {

		Categoria categoria = new Categoria();
		categoria.setId(10L);
		categoria.setNome("Literatura");
		categoria.setDescricao("Nova descrição");

		when(categoriaService.atualizar(any(Long.class), any(Categoria.class)))
				.thenReturn(categoria);

		mockMvc.perform(put("/service/categorias/10")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"nome": "Literatura", "descricao": "Nova descrição"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(10))
			.andExpect(jsonPath("$.nome").value("Literatura"));
	}

	@Test
	public void testDelete() throws Exception {

		mockMvc.perform(delete("/service/categorias/10"))
			.andExpect(status().isNoContent());

		verify(categoriaService).excluir(10L);
	}
}
