package br.edu.bibliotech.controller;

import br.edu.bibliotech.dto.UsuarioLogado;
import br.edu.bibliotech.service.EmprestimoService;
import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MeusEmprestimosController {

    private final EmprestimoService emprestimos;

    public MeusEmprestimosController(EmprestimoService emprestimos) {
        this.emprestimos = emprestimos;
    }

    @GetMapping("/meus-emprestimos")
    public String listar(@SessionAttribute(name = "usuarioLogado", required = false) UsuarioLogado usuario, Model model) {
        if (usuario == null) {
            return "redirect:/login";
        }

        model.addAttribute("emprestimos", emprestimos.listarPorUsuario(usuario.id()));
        model.addAttribute("multaPendente", emprestimos.multaPendente(usuario.id()));
        model.addAttribute("hoje", LocalDate.now());
        return "emprestimo/meus";
    }

    @PostMapping("/meus-emprestimos/{id}/renovar")
    public String renovar(@PathVariable Long id, @SessionAttribute(name = "usuarioLogado", required = false) UsuarioLogado usuario,
            RedirectAttributes atributos) {
        if (usuario == null) {
            return "redirect:/login";
        }

        try {
            emprestimos.renovarDoUsuario(id, usuario.id());
            atributos.addFlashAttribute("sucesso", "Empréstimo renovado por sete dias.");
        } catch (IllegalStateException erro) {
            atributos.addFlashAttribute("erro", erro.getMessage());
        }

        return "redirect:/meus-emprestimos";
    }
}
