package br.edu.ufcg.psoft.arena.turma;

public record TurmaResponse(
        Long id, String nome, String codigo, String periodoLetivo, Long professorId, StatusTurma status) {
}
