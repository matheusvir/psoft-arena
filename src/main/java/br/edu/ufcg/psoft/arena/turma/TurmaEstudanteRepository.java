package br.edu.ufcg.psoft.arena.turma;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TurmaEstudanteRepository extends JpaRepository<TurmaEstudante, Long> {

    Optional<TurmaEstudante> findByTurmaIdAndEstudanteId(Long turmaId, Long estudanteId);

    List<TurmaEstudante> findAllByTurmaId(Long turmaId);

    boolean existsByTurmaIdAndEstudanteId(Long turmaId, Long estudanteId);
}
