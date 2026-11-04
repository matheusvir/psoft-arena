package br.edu.ufcg.psoft.arena.problema.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Entrada comum para criação e edição; status e id não são editáveis pelo cliente. */
public record ProblemaRequest(
        @NotBlank @Size(max = 200) String titulo,
        @NotBlank @Size(max = 2000) String descricao,
        @NotBlank @Size(max = 4000) String requisitosFuncionais,
        @NotBlank @Size(max = 4000) String requisitosProjeto) {
}
