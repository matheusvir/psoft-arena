package br.edu.ufcg.psoft.arena.problema;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import br.edu.ufcg.psoft.arena.problema.dto.ProblemaRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class ProblemaRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void configurarValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void fecharValidator() {
        validatorFactory.close();
    }

    @Test
    void cadaCampoRejeitaValorNuloEmBrancoEAcimaDoLimite() {
        for (Campo campo : campos()) {
            assertCampoInvalido(campo.nome(), null);
            assertCampoInvalido(campo.nome(), " \t\n ");
            assertCampoInvalido(campo.nome(), "x".repeat(campo.limite() + 1));
        }
    }

    @Test
    void limitesV1AceitamValoresExatamenteNoMaximo() {
        ProblemaRequest requisicao = new ProblemaRequest(
                "x".repeat(200),
                "x".repeat(2000),
                "x".repeat(4000),
                "x".repeat(4000));

        assertThat(validator.validate(requisicao)).isEmpty();
    }

    @Test
    void textoNaoBrancoPreservaEspacosTabsENovasLinhas() {
        String requisitosFuncionais = "  RF com espaço inicial\nsegunda linha\r\nfinal  ";
        String requisitosProjeto = "  RP com tab\t e espaço final  ";
        ProblemaRequest requisicao = new ProblemaRequest(
                "  Título  ", "  Descrição\n  ", requisitosFuncionais, requisitosProjeto);

        assertThat(validator.validate(requisicao)).isEmpty();
        assertThat(requisicao.titulo()).isEqualTo("  Título  ");
        assertThat(requisicao.descricao()).isEqualTo("  Descrição\n  ");
        assertThat(requisicao.requisitosFuncionais()).isEqualTo(requisitosFuncionais);
        assertThat(requisicao.requisitosProjeto()).isEqualTo(requisitosProjeto);
    }

    private void assertCampoInvalido(String nome, String valor) {
        ProblemaRequest requisicao = switch (nome) {
            case "titulo" -> new ProblemaRequest(valor, "Descrição", "RF", "RP");
            case "descricao" -> new ProblemaRequest("Título", valor, "RF", "RP");
            case "requisitosFuncionais" -> new ProblemaRequest("Título", "Descrição", valor, "RP");
            case "requisitosProjeto" -> new ProblemaRequest("Título", "Descrição", "RF", valor);
            default -> throw new IllegalArgumentException("Campo desconhecido: " + nome);
        };

        assertThat(validator.validate(requisicao))
                .extracting(violacao -> violacao.getPropertyPath().toString())
                .contains(nome);
    }

    private List<Campo> campos() {
        return List.of(
                new Campo("titulo", 200),
                new Campo("descricao", 2000),
                new Campo("requisitosFuncionais", 4000),
                new Campo("requisitosProjeto", 4000));
    }

    private record Campo(String nome, int limite) {
    }
}
