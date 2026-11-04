package br.edu.ufcg.psoft.arena.problema.dto;

import br.edu.ufcg.psoft.arena.problema.StatusProblema;

/** Representação pública de um problema; não expõe a entidade JPA. */
public record ProblemaResponse(
        Long id,
        String titulo,
        String descricao,
        String requisitosFuncionais,
        String requisitosProjeto,
        StatusProblema status) {
}
