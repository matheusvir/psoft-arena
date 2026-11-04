package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V2__normalizar_estudantes_email_unico extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conexao = context.getConnection();
        List<EstudanteMigrado> estudantes = carregarEValidarEstudantes(conexao);
        atualizarEstudantes(conexao, estudantes);
        try (Statement comando = conexao.createStatement()) {
            comando.execute("alter table estudante add constraint uk_estudante_email unique (email)");
        }
    }

    private List<EstudanteMigrado> carregarEValidarEstudantes(Connection conexao) throws SQLException {
        List<EstudanteMigrado> estudantes = new ArrayList<>();
        Map<String, Long> matriculas = new HashMap<>();
        Map<String, Long> emails = new HashMap<>();

        // Pre-validar toda a carga antes de executar qualquer alteracao.
        try (ValidatorFactory fabricaValidadores = Validation.buildDefaultValidatorFactory();
                Statement consulta = conexao.createStatement();
                ResultSet registros = consulta.executeQuery(
                        "select id, nome, matricula, email from estudante order by id")) {
            Validator validador = fabricaValidadores.getValidator();
            while (registros.next()) {
                long estudanteId = registros.getLong("id");
                String nomeNormalizado = registros.getString("nome").trim();
                String matriculaNormalizada = registros.getString("matricula").trim();
                String emailNormalizado = registros.getString("email").trim().toLowerCase(Locale.ROOT);
                EstudanteMigrado estudante = new EstudanteMigrado(estudanteId, nomeNormalizado, matriculaNormalizada,
                        emailNormalizado);
                Set<ConstraintViolation<EstudanteMigrado>> erros = validador.validate(estudante);
                if (!erros.isEmpty()) {
                    List<String> camposInvalidos = erros.stream()
                            .map(erro -> erro.getPropertyPath().toString()).sorted().toList();
                    throw new SQLException("Estudante " + estudante.id() + " invalido apos normalizacao: "
                            + camposInvalidos);
                }
                verificarDuplicata(matriculas, estudante.matricula(), estudante.id(), "matricula");
                verificarDuplicata(emails, estudante.email(), estudante.id(), "email");
                estudantes.add(estudante);
            }
        }
        return estudantes;
    }

    private void atualizarEstudantes(Connection conexao, List<EstudanteMigrado> estudantes) throws SQLException {
        try (PreparedStatement atualizacao = conexao.prepareStatement(
                "update estudante set nome = ?, matricula = ?, email = ? where id = ?")) {
            for (EstudanteMigrado estudante : estudantes) {
                atualizacao.setString(1, estudante.nome());
                atualizacao.setString(2, estudante.matricula());
                atualizacao.setString(3, estudante.email());
                atualizacao.setLong(4, estudante.id());
                atualizacao.addBatch();
            }
            atualizacao.executeBatch();
        }
    }

    private static void verificarDuplicata(Map<String, Long> valores, String valor, long id, String campo)
            throws SQLException {
        Long anterior = valores.putIfAbsent(valor, id);
        if (anterior != null) {
            throw new SQLException(
                    "Colisao de " + campo + " apos normalizacao nos estudantes " + anterior + " e " + id);
        }
    }

    // Contrato historico da V2: independente dos DTOs que evoluirao com a
    // aplicacao.
    private record EstudanteMigrado(long id,
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Size(max = 20) String matricula,
            @NotBlank @Size(max = 160) @Email String email) {
    }
}
