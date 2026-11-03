package br.edu.ufcg.psoft.arena.turma;

import jakarta.validation.constraints.NotNull;

public record TurmaEstudanteRequest(@NotNull Long estudanteId) {
}
