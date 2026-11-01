package br.edu.ufcg.psoft.arena.turma;

import br.edu.ufcg.psoft.arena.professor.Professor;
import br.edu.ufcg.psoft.arena.shared.entity.BaseEntity;
import br.edu.ufcg.psoft.arena.shared.exception.RegraDeNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "turma", uniqueConstraints = @UniqueConstraint(
        name = "uk_turma_codigo_periodo", columnNames = {"codigo", "periodo_letivo"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Turma extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 40)
    private String codigo;

    @Column(name = "periodo_letivo", nullable = false, length = 20)
    private String periodoLetivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusTurma status = StatusTurma.ATIVA;

    public Turma(String nome, String codigo, String periodoLetivo, Professor professor) {
        atualizar(nome, codigo, periodoLetivo, professor);
    }

    public void atualizar(String nome, String codigo, String periodoLetivo, Professor professor) {
        this.nome = nome;
        this.codigo = codigo;
        this.periodoLetivo = periodoLetivo;
        this.professor = professor;
    }

    public void encerrar() {
        if (status == StatusTurma.ENCERRADA) {
            throw new RegraDeNegocioException("Turma ja encerrada.");
        }
        status = StatusTurma.ENCERRADA;
    }
}
