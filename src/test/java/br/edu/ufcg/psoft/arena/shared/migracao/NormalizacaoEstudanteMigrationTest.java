package br.edu.ufcg.psoft.arena.shared.migracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.stream.Stream;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class NormalizacaoEstudanteMigrationTest {

    @Test
    void migraBancoVazioSemAdicionarColuna() {
        var banco = novoBanco();
        var flyway = Flyway.configure().dataSource(banco).load();
        flyway.migrate();
        var jdbc = new JdbcTemplate(banco);
        var versaoAtual = flyway.info().current().getVersion().getVersion();
        var colunas = jdbc.queryForList("select column_name from information_schema.columns "
                + "where table_name = 'ESTUDANTE'", String.class);
        var reexecucao = flyway.migrate();
        assertThat(versaoAtual).isEqualTo("2");
        assertThat(colunas).containsExactlyInAnyOrder("ID", "NOME", "MATRICULA", "EMAIL");
        assertThat(reexecucao.migrationsExecuted).isZero();
    }

    @Test
    void normalizaV1PreservandoIdentidadeEVinculosEImpedeEmailDuplicado() {
        var banco = novoBanco();
        Flyway.configure().dataSource(banco).target("1").load().migrate();
        var jdbc = new JdbcTemplate(banco);
        inserir(jdbc, 1, "  Ana Silva  ", "  Ab123 ", " Ana@EXAMPLE.COM ");
        jdbc.update("insert into professor (id, nome, email) values (1, 'Professor', 'prof@example.com')");
        jdbc.update("insert into turma (id, nome, codigo, periodo_letivo, professor_id, status) "
                + "values (1, 'Turma', 'T1', '2026.2', 1, 'ATIVA')");
        jdbc.update("insert into turma_estudante (id, turma_id, estudante_id, associado_em) "
                + "values (1, 1, 1, current_timestamp)");
        var flyway = Flyway.configure().dataSource(banco).load();
        flyway.migrate();
        var estudanteMigrado = jdbc.queryForMap("select nome, matricula, email from estudante where id = 1");
        var estudanteVinculado = jdbc.queryForObject("select estudante_id from turma_estudante where id = 1", Long.class);
        assertThat(estudanteMigrado)
                .containsEntry("NOME", "Ana Silva").containsEntry("MATRICULA", "Ab123")
                .containsEntry("EMAIL", "ana@example.com");
        assertThat(estudanteVinculado).isEqualTo(1L);
        assertThatThrownBy(() -> inserir(jdbc, 2, "Outra", "456", "ana@example.com"))
                .isInstanceOf(DataIntegrityViolationException.class);
        var dadosAntesDaReexecucao = jdbc.queryForList("select * from estudante");
        var reexecucao = flyway.migrate();
        var dadosDepoisDaReexecucao = jdbc.queryForList("select * from estudante");
        assertThat(reexecucao.migrationsExecuted).isZero();
        assertThat(dadosDepoisDaReexecucao).isEqualTo(dadosAntesDaReexecucao);
    }

    @ParameterizedTest
    @MethodSource("dadosInvalidos")
    void rejeitaCargaInvalidaAntesDeModificarDados(String nome, String matricula, String email, String campo) {
        var banco = novoBanco();
        Flyway.configure().dataSource(banco).target("1").load().migrate();
        var jdbc = new JdbcTemplate(banco);
        inserir(jdbc, 1, " Ana ", "123", "ana@example.com");
        inserir(jdbc, 2, nome, matricula, email);
        var dadosAntesDaMigracao = jdbc.queryForList("select * from estudante order by id");
        var flyway = Flyway.configure().dataSource(banco).load();
        assertThatThrownBy(flyway::migrate)
                .isInstanceOf(FlywayException.class).hasStackTraceContaining(campo).hasStackTraceContaining("2");
        var dadosDepoisDaFalha = jdbc.queryForList("select * from estudante order by id");
        var restricoesDeEmail = jdbc.queryForObject("select count(*) from information_schema.table_constraints "
                + "where constraint_name = 'UK_ESTUDANTE_EMAIL'", Integer.class);
        assertThat(dadosDepoisDaFalha).isEqualTo(dadosAntesDaMigracao);
        assertThat(restricoesDeEmail).isZero();
    }

    static Stream<Arguments> dadosInvalidos() {
        return Stream.of(
                Arguments.of("Bia", " 123 ", "bia@example.com", "matricula"),
                Arguments.of("Bia", "456", " Ana@EXAMPLE.com ", "email"),
                Arguments.of("   ", "456", "bia@example.com", "nome"),
                Arguments.of("Bia", "   ", "bia@example.com", "matricula"),
                Arguments.of("Bia", "456", "   ", "email"),
                Arguments.of("Bia", "456", "invalido", "email"));
    }

    private DriverManagerDataSource novoBanco() {
        return new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
    }

    private void inserir(JdbcTemplate jdbc, long id, String nome, String matricula, String email) {
        jdbc.update("insert into estudante (id, nome, matricula, email) values (?, ?, ?, ?)", id, nome, matricula, email);
    }
}
