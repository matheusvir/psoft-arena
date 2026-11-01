package br.edu.ufcg.psoft.arena.professor;

import br.edu.ufcg.psoft.arena.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "professor")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Professor extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    public Professor(String nome, String email) {
        this.nome = nome;
        this.email = email;
    }
}
