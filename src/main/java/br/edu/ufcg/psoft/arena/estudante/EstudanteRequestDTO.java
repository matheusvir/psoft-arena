package br.edu.ufcg.psoft.arena.estudante;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EstudanteRequestDTO {

    @NotBlank
    @Size(max = 120)
    @Schema(description = "Nome sem espacos nas extremidades")
    private final String nome;

    @NotBlank
    @Size(max = 20)
    @Schema(description = "Matricula unica apos trim")
    private final String matricula;

    @NotBlank
    @Size(max = 160)
    @Email
    @Schema(description = "E-mail unico, normalizado com trim e minusculas")
    private final String email;

    @JsonCreator
    public EstudanteRequestDTO(@JsonProperty("nome") String nome,
            @JsonProperty("matricula") String matricula, @JsonProperty("email") String email) {
        this.nome = NormalizacaoEstudante.removerEspacosExternos(nome);
        this.matricula = NormalizacaoEstudante.removerEspacosExternos(matricula);
        this.email = NormalizacaoEstudante.normalizarEmail(email);
    }

    public String getNome() {
        return nome;
    }

    public String getMatricula() {
        return matricula;
    }

    public String getEmail() {
        return email;
    }
}
