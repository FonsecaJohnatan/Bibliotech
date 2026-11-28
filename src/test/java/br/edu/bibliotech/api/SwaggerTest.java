package br.edu.bibliotech.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class SwaggerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	public void testOpenApi() throws Exception {
		String resposta = mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andReturn().getResponse().getContentAsString();

		JsonNode paths = objectMapper.readTree(resposta).get("paths");
		assertThat(paths.has("/service/autores")).isTrue();
		assertThat(paths.has("/service/categorias")).isTrue();
		assertThat(paths.has("/service/livros")).isTrue();
		assertThat(paths.has("/service/usuarios")).isTrue();
		assertThat(paths.has("/service/exemplares")).isTrue();
		assertThat(paths.has("/service/emprestimos")).isTrue();
		paths.fieldNames().forEachRemaining(rota -> assertThat(rota).startsWith("/service/"));
	}

	@Test
	public void testSwaggerUi() throws Exception {
		mockMvc.perform(get("/swagger-ui.html"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/swagger-ui/index.html"));

		mockMvc.perform(get("/swagger-ui/index.html"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
	}
}
