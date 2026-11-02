package br.edu.ufcg.psoft.arena.estrategia.dto;

import br.edu.ufcg.psoft.arena.estrategia.model.Estrategia;
import br.edu.ufcg.psoft.arena.estrategia.model.TipoEstrategia;

public record EstrategiaResponse(
        Long id,
        String nome,
        TipoEstrategia tipo,
        String descricao,
        String prompt) {

    public static EstrategiaResponse de(Estrategia estrategia) {
        return new EstrategiaResponse(
                estrategia.getId(),
                estrategia.getNome(),
                estrategia.getTipo(),
                estrategia.getDescricao(),
                estrategia.getPrompt());
    }
}
