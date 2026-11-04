package br.edu.ufcg.psoft.arena.problema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import br.edu.ufcg.psoft.arena.shared.exception.RegraDeNegocioException;

class ProblemaTest {

    @Test
    void problemaNovoNasceAtivo() {
        Problema problema = criarProblema();

        assertThat(problema.getStatus()).isEqualTo(StatusProblema.ATIVO);
    }

    @Test
    void desativarMudaEstadoParaInativo() {
        Problema problema = criarProblema();

        problema.desativar();

        assertThat(problema.getStatus()).isEqualTo(StatusProblema.INATIVO);
    }

    @Test
    void desativarProblemaInativoMantemEstado() {
        Problema problema = criarProblema();
        problema.desativar();

        problema.desativar();

        assertThat(problema.getStatus()).isEqualTo(StatusProblema.INATIVO);
    }

    @Test
    void problemaAtivoPodeSerUsadoPorNovaAtividade() {
        assertThatCode(() -> criarProblema().exigirAtivoParaNovaAtividade())
                .doesNotThrowAnyException();
    }

    @Test
    void problemaInativoNaoPodeSerUsadoPorNovaAtividade() {
        Problema problema = criarProblema();
        problema.desativar();

        assertThatThrownBy(problema::exigirAtivoParaNovaAtividade)
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Problema inativo não pode ser associado a nova atividade.");
    }

    @Test
    void permitirRemocaoQuandoConsultaConfirmaAusenciaDeUso() {
        assertThatCode(() -> criarProblema().validarPodeRemover(false))
                .doesNotThrowAnyException();
    }

    @Test
    void rejeitarRemocaoQuandoConsultaConfirmaUsoHistorico() {
        assertThatThrownBy(() -> criarProblema().validarPodeRemover(true))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Problema já foi utilizado em uma atividade e não pode ser removido.");
    }

    private Problema criarProblema() {
        return new Problema("Título", "Descrição", "Requisitos funcionais", "Requisitos de projeto");
    }
}
