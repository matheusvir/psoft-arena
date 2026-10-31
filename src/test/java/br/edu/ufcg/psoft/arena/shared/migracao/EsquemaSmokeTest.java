package br.edu.ufcg.psoft.arena.shared.migracao;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class EsquemaSmokeTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void matriculaDuplicadaRejeitada() {
        inserirEstudante(1L, "Ana", "2026001", "ana@example.com");

        assertThatThrownBy(() -> inserirEstudante(2L, "Outra Ana", "2026001", "outra@example.com"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void parTurmaEstudanteDuplicadoRejeitado() {
        inserirProfessor(1L, "Professor", "professor@example.com");
        inserirTurma(1L, "Turma A", "T01", "2026.2", 1L);
        inserirEstudante(1L, "Ana", "2026001", "ana@example.com");

        inserirVinculo(1L, 1L, 1L);

        assertThatThrownBy(() -> inserirVinculo(2L, 1L, 1L))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void emailDeProfessorDuplicadoRejeitado() {
        inserirProfessor(1L, "Professor", "professor@example.com");

        assertThatThrownBy(() -> inserirProfessor(2L, "Outro Professor", "professor@example.com"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void professorVinculadoNaoPodeSerExcluido() {
        inserirProfessor(1L, "Professor", "professor@example.com");
        inserirTurma(1L, "Turma A", "T01", "2026.2", 1L);

        assertThatThrownBy(() -> jdbcTemplate.update("delete from professor where id = ?", 1L))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void inserirProfessor(Long id, String nome, String email) {
        jdbcTemplate.update(
                "insert into professor (id, nome, email) values (?, ?, ?)", id, nome, email);
    }

    private void inserirTurma(Long id, String nome, String codigo, String periodoLetivo, Long professorId) {
        jdbcTemplate.update(
                "insert into turma (id, nome, codigo, periodo_letivo, professor_id, status) "
                        + "values (?, ?, ?, ?, ?, 'ATIVA')",
                id, nome, codigo, periodoLetivo, professorId);
    }

    private void inserirEstudante(Long id, String nome, String matricula, String email) {
        jdbcTemplate.update(
                "insert into estudante (id, nome, matricula, email) values (?, ?, ?, ?)",
                id, nome, matricula, email);
    }

    private void inserirVinculo(Long id, Long turmaId, Long estudanteId) {
        jdbcTemplate.update(
                "insert into turma_estudante (id, turma_id, estudante_id, associado_em) values (?, ?, ?, ?)",
                id, turmaId, estudanteId,
                LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
    }
}
