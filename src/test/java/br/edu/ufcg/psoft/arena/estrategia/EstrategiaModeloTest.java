package br.edu.ufcg.psoft.arena.estrategia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import br.edu.ufcg.psoft.arena.estrategia.dto.CriarEstrategiaRequest;
import br.edu.ufcg.psoft.arena.estrategia.model.Estrategia;
import br.edu.ufcg.psoft.arena.estrategia.model.TipoEstrategia;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EstrategiaModeloTest {

    private Validator validator;

    @BeforeEach
    void configurarValidador() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void enumContemOsTresTiposPersistidos() {
        assertThat(TipoEstrategia.values())
                .containsExactly(
                        TipoEstrategia.CODE_SMELLS,
                        TipoEstrategia.ACOPLAMENTO_COESAO_PRINCIPIOS,
                        TipoEstrategia.PADROES_DE_PROJETO);
    }

    @Test
    void entidadeAtualizaOsQuatroDadosDaEstrategia() {
        Estrategia estrategia = new Estrategia(
                "Code Smells",
                TipoEstrategia.CODE_SMELLS,
                "Descricao inicial",
                "Prompt inicial");

        estrategia.atualizar(
                "Principios",
                TipoEstrategia.ACOPLAMENTO_COESAO_PRINCIPIOS,
                "Descricao atualizada",
                "Prompt atualizado");

        assertThat(estrategia.getNome()).isEqualTo("Principios");
        assertThat(estrategia.getTipo()).isEqualTo(TipoEstrategia.ACOPLAMENTO_COESAO_PRINCIPIOS);
        assertThat(estrategia.getDescricao()).isEqualTo("Descricao atualizada");
        assertThat(estrategia.getPrompt()).isEqualTo("Prompt atualizado");
    }

    @Test
    void requestValidoNaoPossuiViolacoes() {
        CriarEstrategiaRequest request = new CriarEstrategiaRequest(
                "Code Smells",
                TipoEstrategia.CODE_SMELLS,
                "Descricao",
                "Prompt");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void requestRejeitaCamposAusentesVaziosOuSomenteEspacos() {
        CriarEstrategiaRequest request = new CriarEstrategiaRequest(" ", null, "", "   ");

        assertThat(validator.validate(request)).hasSize(4);
    }

    @Test
    void requestRejeitaValoresAcimaDosLimitesDoBanco() {
        CriarEstrategiaRequest request = new CriarEstrategiaRequest(
                "n".repeat(121),
                TipoEstrategia.CODE_SMELLS,
                "d".repeat(2001),
                "p".repeat(8001));

        assertThat(validator.validate(request)).hasSize(3);
    }

    @Test
    void enumAceitaCadaTipoPrevistoPeloBanco() {
        assertThatCode(() -> {
            for (TipoEstrategia tipo : TipoEstrategia.values()) {
                new Estrategia("Nome", tipo, "Descricao", "Prompt");
            }
        }).doesNotThrowAnyException();
    }
}
