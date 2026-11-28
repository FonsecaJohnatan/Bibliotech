package br.edu.bibliotech.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

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
class UsuarioControllerTest {

	@Autowired
	private MockMvc mockMvc;

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
	public void deveAbrirFormularioDeUsuario() throws Exception {
		mockMvc.perform(get("/usuario/formulario"))
			.andExpect(status().isOk())
			.andExpect(view().name("usuario/formulario"))
			.andExpect(content().string(containsString("action=\"/usuario/novo\"")))
			.andExpect(content().string(containsString("type=\"password\"")))
			.andExpect(content().string(containsString("Administrador")))
			.andExpect(content().string(containsString("Bibliotecário")))
			.andExpect(content().string(containsString("Leitor")));
	}

	@Test
	public void deveCadastrarUsuarioValido() throws Exception {
		mockMvc.perform(post("/usuario/novo").param("nome", " Maria ")
				.param("email", "MARIA@EMAIL.COM").param("senha", "senhaEmTextoPuro").param("perfil", "LEITOR"))
			.andExpect(redirectedUrl("/home"));

		Usuario usuario = usuarioRepository.findAll().get(0);
		assertThat(usuario.getNome()).isEqualTo("Maria");
		assertThat(usuario.getEmail()).isEqualTo("maria@email.com");
		assertThat(new BCryptPasswordEncoder().matches("senhaEmTextoPuro", usuario.getSenha())).isTrue();

		mockMvc.perform(get("/usuario"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("maria@email.com")))
				.andExpect(content().string(containsString("Leitor")))
				.andExpect(content().string(not(containsString(usuario.getSenha()))))
				.andExpect(content().string(not(containsString("senhaEmTextoPuro"))));
	}

	@Test
	public void deveReexibirFormularioQuandoUsuarioInvalido() throws Exception {
		mockMvc.perform(post("/usuario/novo").param("nome", "").param("email", "invalido")
				.param("senha", "").param("perfil", ""))
			.andExpect(status().isOk())
			.andExpect(view().name("usuario/formulario"))
			.andExpect(model().attributeHasFieldErrors("requisicaoNovoUsuario", "nome", "email", "senha", "perfil"));

		mockMvc.perform(post("/usuario/novo").param("nome", "Maria").param("email", "maria@email.com")
				.param("senha", "senhaEmTextoPuro").param("perfil", "INVALIDO"))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasFieldErrors("requisicaoNovoUsuario", "perfil"))
			.andExpect(content().string(containsString("Selecione um perfil válido")))
			.andExpect(content().string(containsString("Bibliotecário")))
			.andExpect(content().string(not(containsString("senhaEmTextoPuro"))));
		assertThat(usuarioRepository.count()).isZero();
	}

	@Test
	public void deveRejeitarEmailDuplicado() throws Exception {
		salvar();
		mockMvc.perform(post("/usuario/novo").param("nome", "Outra").param("email", "MARIA@EMAIL.COM")
				.param("senha", "senhaEmTextoPuro").param("perfil", "BIBLIOTECARIO"))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasErrors("requisicaoNovoUsuario"))
			.andExpect(content().string(containsString("E-mail já cadastrado")))
			.andExpect(content().string(containsString("<option value=\"BIBLIOTECARIO\" selected=\"selected\">Bibliotecário</option>")))
			.andExpect(content().string(not(containsString("senhaEmTextoPuro"))));
		assertThat(usuarioRepository.count()).isEqualTo(1);
	}

	@Test
	public void deveAtualizarUsuario() throws Exception {
		Usuario usuario = salvar();
		mockMvc.perform(get("/usuario/" + usuario.getId() + "/editar"))
			.andExpect(status().isOk())
			.andExpect(view().name("usuario/edicao"))
			.andExpect(content().string(containsString("value=\"Maria\"")))
			.andExpect(content().string(not(containsString("name=\"senha\""))))
			.andExpect(content().string(not(containsString(usuario.getSenha()))));

		mockMvc.perform(post("/usuario/" + usuario.getId()).param("nome", "")
				.param("email", "invalido"))
			.andExpect(view().name("usuario/edicao"))
			.andExpect(model().attributeHasFieldErrors("requisicaoAtualizarUsuario", "nome", "email"))
			.andExpect(content().string(containsString("action=\"/usuario/" + usuario.getId() + "\"")));

		mockMvc.perform(post("/usuario/" + usuario.getId()).param("nome", "Maria atualizada")
				.param("email", "NOVO@EMAIL.COM").param("senha", "outraSenha").param("perfil", "ADMIN"))
			.andExpect(redirectedUrl("/home"));
		Usuario atualizado = usuarioRepository.findById(usuario.getId()).orElseThrow();
		assertThat(atualizado.getNome()).isEqualTo("Maria atualizada");
		assertThat(atualizado.getEmail()).isEqualTo("novo@email.com");
		assertThat(atualizado.getSenha()).isEqualTo(usuario.getSenha());
		assertThat(atualizado.getPerfil()).isEqualTo(Perfil.LEITOR);
	}

	@Test
	public void deveRejeitarEmailDuplicadoNaEdicao() throws Exception {
		Usuario usuario = salvar();
		usuarioService.cadastrar(new Usuario("João", "joao@email.com", "senha", Perfil.LEITOR));
		mockMvc.perform(post("/usuario/" + usuario.getId()).param("nome", "Maria")
				.param("email", "JOAO@EMAIL.COM"))
			.andExpect(status().isOk())
			.andExpect(view().name("usuario/edicao"))
			.andExpect(model().attributeHasErrors("requisicaoAtualizarUsuario"))
			.andExpect(content().string(containsString("E-mail já cadastrado")))
			.andExpect(content().string(containsString("action=\"/usuario/" + usuario.getId() + "\"")));
		assertThat(usuarioRepository.findById(usuario.getId()).orElseThrow().getEmail()).isEqualTo("maria@email.com");
	}

	@Test
	public void deveDesativarUsuario() throws Exception {
		Usuario usuario = salvar();
		mockMvc.perform(post("/usuario/" + usuario.getId() + "/desativar"))
			.andExpect(redirectedUrl("/home"));
		assertThat(usuarioRepository.findById(usuario.getId()).orElseThrow().getAtivo()).isFalse();
		mockMvc.perform(get("/usuario"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Inativo")))
			.andExpect(content().string(not(containsString("/desativar"))));
	}

	@Test
	public void deveRetornar404QuandoUsuarioNaoExiste() throws Exception {
		mockMvc.perform(get("/usuario/999999/editar"))
			.andExpect(status().isNotFound()).andExpect(view().name("erro"));
		mockMvc.perform(post("/usuario/999999").param("nome", "Maria").param("email", "maria@email.com"))
			.andExpect(status().isNotFound()).andExpect(view().name("erro"));
		mockMvc.perform(post("/usuario/999999/desativar"))
			.andExpect(status().isNotFound()).andExpect(view().name("erro"));
	}
}
