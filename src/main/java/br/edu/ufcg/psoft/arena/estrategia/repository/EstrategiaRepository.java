package br.edu.ufcg.psoft.arena.estrategia.repository;

import br.edu.ufcg.psoft.arena.estrategia.model.Estrategia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstrategiaRepository extends JpaRepository<Estrategia, Long> {
}
