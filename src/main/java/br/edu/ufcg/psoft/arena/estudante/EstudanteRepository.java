package br.edu.ufcg.psoft.arena.estudante;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EstudanteRepository extends JpaRepository<Estudante, Long> {

    Optional<Estudante> findByMatricula(String matricula);

    Optional<Estudante> findByEmail(String email);
}
