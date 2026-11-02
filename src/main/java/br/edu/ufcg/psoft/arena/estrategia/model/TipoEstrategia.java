package br.edu.ufcg.psoft.arena.estrategia.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TipoEstrategia {
    CODE_SMELLS,
    ACOPLAMENTO_COESAO_PRINCIPIOS,
    PADROES_DE_PROJETO;

    @JsonCreator
    public static TipoEstrategia de(String valor) {
        return valor == null ? null : TipoEstrategia.valueOf(valor);
    }
}
