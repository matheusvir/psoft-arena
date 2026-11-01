package br.edu.ufcg.psoft.arena.estudante;

import java.util.Locale;

final class NormalizacaoEstudante {

    private NormalizacaoEstudante() {
    }

    static String removerEspacosExternos(String valor) {
        return valor == null ? null : valor.trim();
    }

    static String normalizarEmail(String email) {
        var emailSemEspacosExternos = removerEspacosExternos(email);
        return emailSemEspacosExternos == null ? null : emailSemEspacosExternos.toLowerCase(Locale.ROOT);
    }
}
