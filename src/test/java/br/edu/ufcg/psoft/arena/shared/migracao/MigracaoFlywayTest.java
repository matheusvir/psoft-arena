package br.edu.ufcg.psoft.arena.shared.migracao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class MigracaoFlywayTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void v1AplicadaComSucesso() {
        var atual = flyway.info().current();

        assertThat(atual).isNotNull();
        assertThat(atual.getVersion().getVersion()).isEqualTo("1");
        assertThat(atual.getState()).isEqualTo(MigrationState.SUCCESS);
    }

    @Test
    void tabelasDaEtapa1Existem() {
        List<String> tabelas = jdbcTemplate.queryForList(
                "select table_name from information_schema.tables where table_schema = 'PUBLIC'",
                String.class).stream().map(String::toLowerCase).toList();

        assertThat(tabelas).contains(
                "professor", "problema", "estudante", "estrategia", "turma", "turma_estudante");
    }

    @Test
    void colunasEssenciaisDoVinculoExistem() {
        List<String> colunas = jdbcTemplate.queryForList(
                "select column_name from information_schema.columns where table_name = 'TURMA_ESTUDANTE'",
                String.class).stream().map(String::toLowerCase).toList();

        assertThat(colunas).contains("turma_id", "estudante_id", "associado_em");
    }
}
