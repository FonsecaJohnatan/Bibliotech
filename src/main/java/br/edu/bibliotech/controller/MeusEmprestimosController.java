package br.edu.bibliotech.controller;

import br.edu.bibliotech.model.Usuario;
import br.edu.bibliotech.repository.UsuarioRepository;
import br.edu.bibliotech.service.EmprestimoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MeusEmprestimosController {

    private final UsuarioRepository usuarios;
    private final EmprestimoService emprestimos;

    public MeusEmprestimosController(UsuarioRepository usuarios, EmprestimoService emprestimos) {
        this.usuarios = usuarios;
        this.emprestimos = emprestimos;
    }

    @GetMapping("/meus-emprestimos")
    public String listar(Authentication autenticacao, Model model) {
        Usuario usuario = usuarios.findByEmail(autenticacao.getName())
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        model.addAttribute("emprestimos", emprestimos.listarPorUsuario(usuario.getId()));
        return "emprestimo/meus";
    }
}
