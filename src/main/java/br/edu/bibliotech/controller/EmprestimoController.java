package br.edu.bibliotech.controller;

import br.edu.bibliotech.dto.VerificacaoEmprestimo;
import org.springframework.web.bind.annotation.ResponseBody;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.edu.bibliotech.dto.RequisicaoNovoEmprestimo;
import br.edu.bibliotech.model.Exemplar;
import br.edu.bibliotech.model.Emprestimo;
import br.edu.bibliotech.model.StatusExemplar;
import br.edu.bibliotech.model.Usuario;
import br.edu.bibliotech.service.EmprestimoService;
import br.edu.bibliotech.service.ExemplarService;
import br.edu.bibliotech.service.UsuarioService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/emprestimo")
public class EmprestimoController {

    @Autowired
    private EmprestimoService emprestimoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ExemplarService exemplarService;

    @GetMapping
    public String listar(@RequestParam(required = false) Long usuarioId,
            @RequestParam(defaultValue = "") String situacao, Model model) {
        if (usuarioId != null) {
            usuarioService.buscarPorId(usuarioId);
        }

        model.addAttribute("emprestimos", emprestimoService.filtrar(usuarioId, situacao));
        model.addAttribute("usuarios", usuarioService.listar());
        model.addAttribute("usuarioId", usuarioId);
        model.addAttribute("situacao", situacao);
        model.addAttribute("hoje", LocalDate.now());
        return "emprestimo/lista";
    }

    @GetMapping("verificar")
    @ResponseBody
    public VerificacaoEmprestimo verificar(@RequestParam Long usuarioId) {
        return emprestimoService.verificarUsuario(usuarioId);
    }

    @GetMapping("formulario")
    public String formulario(RequisicaoNovoEmprestimo requisicao, Model model) {
        carregarOpcoes(model, requisicao);
        return "emprestimo/formulario";
    }

    @PostMapping("novo")
    public String novo(@Valid RequisicaoNovoEmprestimo requisicao, BindingResult result, Model model) {
        if(result.hasErrors()) {
            carregarOpcoes(model, requisicao);
            return "emprestimo/formulario";
        }

        try {
            this.emprestimoService.registrar(requisicao.getUsuarioId(), requisicao.getExemplarId());
        } catch (IllegalArgumentException | IllegalStateException | EntityNotFoundException exception) {
            result.reject("cadastro", exception.getMessage());
            carregarOpcoes(model, requisicao);
            return "emprestimo/formulario";
        }

        return "redirect:/emprestimo";
    }

    @PostMapping("/{id}/renovar")
    public String renovar(@PathVariable Long id, RedirectAttributes atributos) {
        try {
            this.emprestimoService.renovar(id);
        } catch (IllegalStateException exception) {
            atributos.addFlashAttribute("erro", exception.getMessage());
            return "redirect:/emprestimo";
        }

        return "redirect:/emprestimo";
    }

    @PostMapping("/{id}/devolver")
    public String devolver(@PathVariable Long id, RedirectAttributes atributos) {
        try {
            this.emprestimoService.devolver(id);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            atributos.addFlashAttribute("erro", exception.getMessage());
            return "redirect:/emprestimo";
        }

        return "redirect:/emprestimo";
    }

    @GetMapping("/{id}/devolucao")
    public String confirmarDevolucao(@PathVariable Long id, Model model) {
        model.addAttribute("emprestimo", emprestimoService.buscarPorId(id));
        model.addAttribute("hoje", LocalDate.now());
        return "emprestimo/devolucao";
    }

    private void carregarOpcoes(Model model, RequisicaoNovoEmprestimo requisicao) {
        List<Usuario> usuarios = usuarioService.listar();
        List<Exemplar> exemplares = exemplarService.listar().stream()
                .filter(exemplar -> exemplar.getStatus() == StatusExemplar.DISPONIVEL)
                .toList();

        model.addAttribute("usuarios", usuarios);
        model.addAttribute("exemplares", exemplares);
        model.addAttribute("dataPrevista", LocalDate.now().plusDays(Emprestimo.PRAZO_DIAS));
        carregarVerificacao(model, requisicao, usuarios, exemplares);
    }

    private void carregarVerificacao(Model model, RequisicaoNovoEmprestimo requisicao,
            List<Usuario> usuarios, List<Exemplar> exemplares) {
        boolean usuarioExiste = usuarios.stream()
                .anyMatch(usuario -> usuario.getId().equals(requisicao.getUsuarioId()));
        boolean exemplarDisponivel = exemplares.stream()
                .anyMatch(exemplar -> exemplar.getId().equals(requisicao.getExemplarId()));
        var verificacao = usuarioExiste ? emprestimoService.verificarUsuario(requisicao.getUsuarioId()) : null;

        model.addAttribute("verificacao", verificacao);
        model.addAttribute("podeConfirmar", verificacao != null && verificacao.permitido() && exemplarDisponivel);
    }
}
