package br.edu.ufcg.psoft.arena.turma;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TurmaRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Size(max = 40) String codigo,
        @NotBlank @Size(max = 20) String periodoLetivo,
        @NotNull Long professorId) {
}
