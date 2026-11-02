package br.edu.ufcg.psoft.arena.estrategia.model;

import br.edu.ufcg.psoft.arena.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "estrategia")
public class Estrategia extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoEstrategia tipo;

    @Column(nullable = false, length = 2000)
    private String descricao;

    @Column(nullable = false, length = 8000)
    private String prompt;

    protected Estrategia() {
    }

    public Estrategia(String nome, TipoEstrategia tipo, String descricao, String prompt) {
        atualizar(nome, tipo, descricao, prompt);
    }

    public void atualizar(String nome, TipoEstrategia tipo, String descricao, String prompt) {
        this.nome = nome;
        this.tipo = tipo;
        this.descricao = descricao;
        this.prompt = prompt;
    }
}
