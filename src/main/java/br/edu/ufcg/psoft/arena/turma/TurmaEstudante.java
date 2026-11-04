package br.edu.ufcg.psoft.arena.turma;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import br.edu.ufcg.psoft.arena.estudante.Estudante;
import br.edu.ufcg.psoft.arena.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "turma_estudante", uniqueConstraints = @UniqueConstraint(
        name = "uk_turma_estudante_par", columnNames = {"turma_id", "estudante_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TurmaEstudante extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turma_id", nullable = false)
    private Turma turma;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudante_id", nullable = false)
    private Estudante estudante;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "associado_em", nullable = false, updatable = false)
    private Instant associadoEm;

    public TurmaEstudante(Turma turma, Estudante estudante, Instant associadoEm) {
        this.turma = turma;
        this.estudante = estudante;
        this.associadoEm = associadoEm;
    }
}
