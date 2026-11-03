package br.edu.ufcg.psoft.arena.turma;

import java.time.Instant;

public record TurmaEstudanteResponse(
        Long estudanteId, String nome, String matricula, Instant associadoEm) {
}
