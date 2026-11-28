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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.bibliotech.model.Perfil;
import br.edu.bibliotech.model.Usuario;
import br.edu.bibliotech.repository.UsuarioRepository;
import br.edu.bibliotech.service.UsuarioService;

@SpringBootTest(properties = {
	"spring.datasource.url=jdbc:h2:mem:usuarios-web;DB_CLOSE_DELAY=-1",
	"spring.jpa.hibernate.ddl-auto=create-drop",
	"spring.jpa.open-in-view=false",
	"spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc(addFilters = false)
class UsuarioRestTest {
	@Test
	void deveRetornar404QuandoListaEstaVazia() throws Exception {

		var resposta = mockMvc.perform(get("/service/usuarios"));

		resposta.andExpect(status().isNotFound());
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private UsuarioService usuarioService;

	@BeforeEach
	public void preparar() {
		usuarioRepository.deleteAll();
	}

	private Usuario salvar() {
		return usuarioService.cadastrar(new Usuario("Maria", "maria@email.com", "senhaEmTextoPuro", Perfil.LEITOR));
	}

	@Test
	public void testSave() throws Exception {
		String json = objectMapper.writeValueAsString(Map.of("nome", " Maria ",
				"email", "MARIA@EMAIL.COM", "senha", "senhaEmTextoPuro", "perfil", "LEITOR"));

		String resposta = mockMvc.perform(post("/service/usuarios")
				.contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isCreated())
			.andExpect(header().doesNotExist("Location"))
			.andExpect(jsonPath("$.nome").value("Maria"))
			.andExpect(jsonPath("$.email").value("maria@email.com"))
			.andExpect(jsonPath("$.perfil").value("LEITOR"))
			.andExpect(jsonPath("$.ativo").value(true))
			.andExpect(jsonPath("$.senha").doesNotExist())
			.andExpect(jsonPath("$.emprestimos").doesNotExist())
			.andReturn().getResponse().getContentAsString();

		Long id = objectMapper.readTree(resposta).get("id").asLong();
		Usuario usuario = usuarioRepository.findById(id).orElseThrow();
		assertThat(new BCryptPasswordEncoder().matches("senhaEmTextoPuro", usuario.getSenha())).isTrue();
		assertThat(resposta).doesNotContain(usuario.getSenha(), "senhaEmTextoPuro");

		mockMvc.perform(get("/service/usuarios/" + id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nome").value("Maria"))
			.andExpect(jsonPath("$.senha").doesNotExist())
			.andExpect(jsonPath("$.emprestimos").doesNotExist());
		mockMvc.perform(get("/service/usuarios"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].email").value("maria@email.com"))
			.andExpect(jsonPath("$[0].senha").doesNotExist())
			.andExpect(jsonPath("$[0].emprestimos").doesNotExist());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{}",
			"{\"nome\":\"Maria\",\"email\":\"invalido\",\"senha\":\"senha\",\"perfil\":\"LEITOR\"}",
			"{\"nome\":\"Maria\",\"email\":\"maria@email.com\",\"perfil\":\"LEITOR\"}",
			"{\"nome\":\"Maria\",\"email\":\"maria@email.com\",\"senha\":\"senha\"}",
			"{\"nome\":\"Maria\",\"email\":\"maria@email.com\",\"senha\":\"senha\",\"perfil\":\"INVALIDO\"}"
	})
	void deveRejeitarCadastroDeUsuarioInvalido(String json) throws Exception {

		var resposta = mockMvc.perform(post("/service/usuarios")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));

		resposta.andExpect(status().isBadRequest());
		assertThat(usuarioRepository.count()).isZero();
	}

	@Test
	public void testSaveEmailDuplicado() throws Exception {
		salvar();
		String json = objectMapper.writeValueAsString(Map.of("nome", "Outra",
				"email", "MARIA@EMAIL.COM", "senha", "senha", "perfil", "BIBLIOTECARIO"));
		mockMvc.perform(post("/service/usuarios").contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.mensagem").value("E-mail já cadastrado"));
		assertThat(usuarioRepository.count()).isEqualTo(1);
	}

	@Test
	public void testUpdate() throws Exception {
		Usuario usuario = salvar();
		String json = objectMapper.writeValueAsString(Map.of("nome", "Maria atualizada", "email", "NOVO@EMAIL.COM"));
		mockMvc.perform(put("/service/usuarios/" + usuario.getId())
				.contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nome").value("Maria atualizada"))
			.andExpect(jsonPath("$.email").value("novo@email.com"))
			.andExpect(jsonPath("$.senha").doesNotExist());

		Usuario atualizado = usuarioRepository.findById(usuario.getId()).orElseThrow();
		assertThat(atualizado.getNome()).isEqualTo("Maria atualizada");
		assertThat(atualizado.getSenha()).isEqualTo(usuario.getSenha());
		assertThat(atualizado.getPerfil()).isEqualTo(Perfil.LEITOR);
		assertThat(atualizado.getAtivo()).isTrue();
	}

	@Test
	public void testUpdateInvalido() throws Exception {
		Usuario usuario = salvar();
		usuarioService.cadastrar(new Usuario("João", "joao@email.com", "senha", Perfil.LEITOR));

		mockMvc.perform(put("/service/usuarios/" + usuario.getId()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"\",\"email\":\"invalido\"}"))
			.andExpect(status().isBadRequest());
		mockMvc.perform(put("/service/usuarios/" + usuario.getId()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Maria\",\"email\":\"JOAO@EMAIL.COM\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.mensagem").value("E-mail já cadastrado"));
		assertThat(usuarioRepository.findById(usuario.getId()).orElseThrow().getEmail()).isEqualTo("maria@email.com");
	}

	@Test
	public void testUpdatePreservaDados() throws Exception {
		Usuario usuario = salvar();
		usuarioService.desativar(usuario.getId());
		String json = objectMapper.writeValueAsString(Map.of("nome", "Maria", "email", "maria@email.com",
				"senha", "outraSenha", "perfil", "ADMIN", "ativo", true));
		mockMvc.perform(put("/service/usuarios/" + usuario.getId())
				.contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.perfil").value("LEITOR"))
			.andExpect(jsonPath("$.ativo").value(false));
		assertThat(usuarioRepository.findById(usuario.getId()).orElseThrow().getSenha()).isEqualTo(usuario.getSenha());
	}

	@Test
	public void testDelete() throws Exception {
		Usuario usuario = salvar();
		mockMvc.perform(delete("/service/usuarios/" + usuario.getId()))
			.andExpect(status().isNoContent());
		assertThat(usuarioRepository.findById(usuario.getId()).orElseThrow().getAtivo()).isFalse();
		mockMvc.perform(get("/service/usuarios/" + usuario.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.ativo").value(false));
	}

	@Test
	public void testFindByIdInexistente() throws Exception {
		mockMvc.perform(get("/service/usuarios/999999")).andExpect(status().isNotFound());
		mockMvc.perform(delete("/service/usuarios/999999")).andExpect(status().isNotFound());
		mockMvc.perform(put("/service/usuarios/999999").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Maria\",\"email\":\"maria@email.com\"}"))
			.andExpect(status().isNotFound());
	}
}
