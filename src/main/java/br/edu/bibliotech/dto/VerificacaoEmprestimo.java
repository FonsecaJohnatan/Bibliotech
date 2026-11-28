package br.edu.bibliotech.dto;

import java.time.LocalDate;

public record VerificacaoEmprestimo(boolean usuarioAtivo, boolean semMulta,
        boolean semAtraso, boolean abaixoDoLimite, LocalDate dataPrevista) {

    public boolean permitido() {
        return usuarioAtivo && semMulta && semAtraso && abaixoDoLimite;
    }
}
