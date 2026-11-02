package br.edu.ufcg.psoft.arena.estrategia.dto;

import br.edu.ufcg.psoft.arena.estrategia.model.TipoEstrategia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AtualizarEstrategiaRequest(
        @NotBlank(message = "nome obrigatorio")
        @Size(max = 120, message = "nome deve ter no maximo 120 caracteres")
        String nome,

        @NotNull(message = "tipo obrigatorio")
        TipoEstrategia tipo,

        @NotBlank(message = "descricao obrigatoria")
        @Size(max = 2000, message = "descricao deve ter no maximo 2000 caracteres")
        String descricao,

        @NotBlank(message = "prompt obrigatorio")
        @Size(max = 8000, message = "prompt deve ter no maximo 8000 caracteres")
        String prompt) {
}
