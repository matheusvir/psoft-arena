package br.edu.ufcg.psoft.arena.estudante;

import br.edu.ufcg.psoft.arena.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

@Getter
@Entity
@Table(name = "estudante", uniqueConstraints = {
        @UniqueConstraint(name = "uk_estudante_matricula", columnNames = "matricula"),
        @UniqueConstraint(name = "uk_estudante_email", columnNames = "email")
})
public class Estudante extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 20)
    private String matricula;

    @Column(nullable = false, length = 160)
    private String email;

    protected Estudante() {
    }

    public Estudante(String nome, String matricula, String email) {
        atualizarDados(nome, matricula, email);
    }

    public void atualizarDados(String nome, String matricula, String email) {
        this.nome = NormalizacaoEstudante.removerEspacosExternos(nome);
        this.matricula = NormalizacaoEstudante.removerEspacosExternos(matricula);
        this.email = NormalizacaoEstudante.normalizarEmail(email);
    }
}
