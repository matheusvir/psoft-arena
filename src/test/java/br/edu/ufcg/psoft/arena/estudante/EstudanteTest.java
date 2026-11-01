package br.edu.ufcg.psoft.arena.estudante;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EstudanteTest {

    @Test
    void normalizaChamadasInternasDeCriacaoEAtualizacao() {
        var estudante = new Estudante(" Ana  Silva ", " Ab123 ", " ANA@EXAMPLE.com ");
        assertThat(estudante.getNome()).isEqualTo("Ana  Silva");
        assertThat(estudante.getMatricula()).isEqualTo("Ab123");
        assertThat(estudante.getEmail()).isEqualTo("ana@example.com");

        estudante.atualizarDados(" Bia ", " Cd456 ", " BIA@EXAMPLE.com ");
        assertThat(estudante.getNome()).isEqualTo("Bia");
        assertThat(estudante.getMatricula()).isEqualTo("Cd456");
        assertThat(estudante.getEmail()).isEqualTo("bia@example.com");
    }

    @Test
    void mantemCamposNulosDisponiveisParaValidacao() {
        var dados = new EstudanteRequestDTO(null, null, null);
        assertThat(dados.getNome()).isNull();
        assertThat(dados.getMatricula()).isNull();
        assertThat(dados.getEmail()).isNull();
    }
}
