package br.edu.bibliotech.dto;

import br.edu.bibliotech.model.Perfil;

public record UsuarioLogado(Long id, String nome, Perfil perfil) {

    public boolean administrador() {
        return perfil == Perfil.ADMIN;
    }

    public boolean podeGerenciar() {
        return perfil == Perfil.ADMIN || perfil == Perfil.BIBLIOTECARIO;
    }
}
