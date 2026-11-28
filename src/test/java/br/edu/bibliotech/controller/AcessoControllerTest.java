package br.edu.bibliotech.controller;

import br.edu.bibliotech.model.Perfil;
import br.edu.bibliotech.model.Categoria;
import br.edu.bibliotech.model.Exemplar;
import br.edu.bibliotech.model.Livro;
import br.edu.bibliotech.model.Usuario;
import br.edu.bibliotech.repository.CategoriaRepository;
import br.edu.bibliotech.repository.ExemplarRepository;
import br.edu.bibliotech.repository.LivroRepository;
import br.edu.bibliotech.repository.UsuarioRepository;
import br.edu.bibliotech.service.EmprestimoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = "spring.jpa.show-sql=false")
@AutoConfigureMockMvc
@Transactional
class AcessoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private CategoriaRepository categorias;

    @Autowired
    private LivroRepository livros;

    @Autowired
    private ExemplarRepository exemplares;

    @Autowired
    private EmprestimoService emprestimos;

    @Autowired
    private BCryptPasswordEncoder codificadorSenha;

    @Test
    void deveAbrirCatalogoSemLogin() throws Exception {

        var resposta = mockMvc.perform(get("/livro"));

        resposta.andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Novo livro"))));
    }

    @Test
    void deveIncluirTokenCsrfNoFormularioDeLogin() throws Exception {

        var resposta = mockMvc.perform(get("/login"));

        resposta.andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void devePedirLoginParaAbrirMeusEmprestimos() throws Exception {

        var resposta = mockMvc.perform(get("/meus-emprestimos").accept(MediaType.TEXT_HTML));

        resposta.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "leitor@email.com", roles = "LEITOR")
    void deveBloquearLeitorNoCadastroDeLivro() throws Exception {

        var resposta = mockMvc.perform(get("/livro/formulario"));

        resposta.andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "leitor@email.com", roles = "LEITOR")
    void deveBloquearLeitorNaApiDeUsuarios() throws Exception {

        var resposta = mockMvc.perform(get("/service/usuarios"));

        resposta.andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "leitor@email.com", roles = "LEITOR")
    void deveOcultarMenuAdministrativoDoLeitor() throws Exception {

        var resposta = mockMvc.perform(get("/home"));

        resposta.andExpect(status().isOk())
                .andExpect(content().string(containsString("Meus empréstimos")))
                .andExpect(content().string(not(containsString("href=\"/usuario\""))));
    }

    @Test
    void deveAutenticarUsuarioComSenhaCadastrada() throws Exception {
        String senha = codificadorSenha.encode("senha-segura");
        usuarios.save(new Usuario("Leitor", "login@email.com", senha, Perfil.LEITOR));

        var resposta = mockMvc.perform(post("/login").with(csrf())
                .param("email", "login@email.com")
                .param("password", "senha-segura"));

        resposta.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    @Test
    void deveMostrarDetalheDoLivroSemLogin() throws Exception {
        Categoria categoria = categorias.save(new Categoria("Romance", null));
        Livro livro = livros.save(new Livro("Dom Casmurro", "9780110002224", 1899, categoria));
        exemplares.save(new Exemplar("TOMBO-1", livro));

        var resposta = mockMvc.perform(get("/livro/" + livro.getId()));

        resposta.andExpect(status().isOk())
                .andExpect(content().string(containsString("Dom Casmurro")))
                .andExpect(content().string(containsString("TOMBO-1")));
    }

    @Test
    void deveRetornar404QuandoLivroNaoExiste() throws Exception {

        var resposta = mockMvc.perform(get("/livro/999999"));

        resposta.andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "bibliotecario@email.com", roles = "BIBLIOTECARIO")
    void devePermitirBibliotecarioNoCadastroDeLivro() throws Exception {

        var resposta = mockMvc.perform(get("/livro/formulario"));

        resposta.andExpect(status().isOk())
                .andExpect(view().name("livro/formulario"));
    }

    @Test
    @WithMockUser(username = "leitor@email.com", roles = "LEITOR")
    void deveMostrarSomenteEmprestimosDoUsuarioAutenticado() throws Exception {
        Usuario leitor = usuarios.save(new Usuario("Leitor", "leitor@email.com", "hash-teste", Perfil.LEITOR));
        Usuario outro = usuarios.save(new Usuario("Outro", "outro@email.com", "hash-teste", Perfil.LEITOR));
        Categoria categoria = categorias.save(new Categoria("Romance", null));
        Livro livroDoLeitor = livros.save(new Livro("Livro do leitor", "9780110002224", 1899, categoria));
        Livro livroDoOutro = livros.save(new Livro("Livro de outro", "9780110002231", 1900, categoria));
        Exemplar exemplarDoLeitor = exemplares.save(new Exemplar("TOMBO-1", livroDoLeitor));
        Exemplar exemplarDoOutro = exemplares.save(new Exemplar("TOMBO-2", livroDoOutro));
        emprestimos.registrar(leitor.getId(), exemplarDoLeitor.getId());
        emprestimos.registrar(outro.getId(), exemplarDoOutro.getId());

        var resposta = mockMvc.perform(get("/meus-emprestimos"));

        resposta.andExpect(status().isOk())
                .andExpect(content().string(containsString("Livro do leitor")))
                .andExpect(content().string(not(containsString("Livro de outro"))));
    }
}
