package br.edu.ufcg.psoft.arena.estrategia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.ufcg.psoft.arena.estrategia.model.Estrategia;
import br.edu.ufcg.psoft.arena.estrategia.model.TipoEstrategia;
import br.edu.ufcg.psoft.arena.estrategia.repository.EstrategiaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class EstrategiaContratoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EstrategiaRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    private Estrategia original;

    @BeforeEach
    void preparar() {
        repository.deleteAll();
        original = repository.save(new Estrategia(
                "Original", TipoEstrategia.CODE_SMELLS, "Descricao original", "Prompt original"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "2", "3", "-1", "0.0", "\"0\"", "\"1\"", "\"2\"",
            "\"TIPO_INVALIDO\"", "\"code_smells\"", "\" CODE_SMELLS \"", "true", "[]", "{}"})
    void rejeitaTipoForaDoContratoEmPostEPut(String tipoJson) throws Exception {
        ObjectNode corpo = corpoValido();
        corpo.set("tipo", objectMapper.readTree(tipoJson));
        verificarRejeicaoSemAlterarBanco(corpo.toString());
    }

    @ParameterizedTest
    @MethodSource("corposInvalidos")
    void rejeitaCamposInvalidosEmPostEPutSemAlterarBanco(String corpo) throws Exception {
        verificarRejeicaoSemAlterarBanco(corpo);
    }

    static Stream<String> corposInvalidos() {
        return Stream.of("nome", "tipo", "descricao", "prompt").flatMap(campo -> {
            ObjectNode ausente = corpoValido();
            ausente.remove(campo);
            ObjectNode nulo = corpoValido();
            nulo.putNull(campo);
            if (campo.equals("tipo")) {
                return Stream.of(ausente.toString(), nulo.toString());
            }
            int limite = switch (campo) {
                case "nome" -> 120;
                case "descricao" -> 2000;
                default -> 8000;
            };
            ObjectNode vazio = corpoValido();
            vazio.put(campo, "");
            ObjectNode branco = corpoValido();
            branco.put(campo, "   ");
            ObjectNode longo = corpoValido();
            longo.put(campo, "x".repeat(limite + 1));
            return Stream.of(ausente.toString(), nulo.toString(), vazio.toString(),
                    branco.toString(), longo.toString());
        });
    }

    @Test
    void aceitaLimitesExatosEmPostEPutEPersistePrompt() throws Exception {
        ObjectNode corpo = corpoValido();
        corpo.put("nome", "n".repeat(120));
        corpo.put("descricao", "d".repeat(2000));
        corpo.put("prompt", "p".repeat(8000));

        String resposta = mockMvc.perform(post("/api/v1/estrategias")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo.toString()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(resposta).get("id").asLong();
        assertThat(repository.findById(id).orElseThrow().getPrompt()).isEqualTo(corpo.get("prompt").asText());

        mockMvc.perform(put("/api/v1/estrategias/{id}", original.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(original.getId()))
                .andExpect(jsonPath("$.nome").value(corpo.get("nome").asText()))
                .andExpect(jsonPath("$.descricao").value(corpo.get("descricao").asText()))
                .andExpect(jsonPath("$.prompt").value(corpo.get("prompt").asText()));
        assertThat(repository.findById(original.getId()).orElseThrow().getPrompt())
                .isEqualTo(corpo.get("prompt").asText());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{", "null"})
    void corpoAusenteOuMalformadoRetornaProblemDetail(String corpo) throws Exception {
        verificarRejeicaoSemAlterarBanco(corpo);
    }

    @Test
    void idMalformadoRetornaProblemDetail() throws Exception {
        verificarProblemDetail(mockMvc.perform(get("/api/v1/estrategias/abc")));
        verificarProblemDetail(mockMvc.perform(put("/api/v1/estrategias/abc")
                .contentType(MediaType.APPLICATION_JSON).content(corpoValido().toString())));
    }

    @Test
    void openApiDescreveStatusSchemasErrosELocation() throws Exception {
        String resposta = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode spec = objectMapper.readTree(resposta);
        JsonNode paths = spec.path("paths");
        JsonNode criacao = paths.path("/api/v1/estrategias").path("post").path("responses");
        assertThat(criacao.has("200")).isFalse();
        assertThat(criacao.path("201").path("headers").has("Location")).isTrue();
        assertThat(criacao.path("201").path("content").path("application/json")
                .path("schema").path("$ref").asText()).isEqualTo("#/components/schemas/EstrategiaResponse");
        verificarErroDocumentado(criacao, "400");

        for (String metodo : new String[]{"get", "put"}) {
            JsonNode respostas = paths.path("/api/v1/estrategias/{id}").path(metodo).path("responses");
            assertThat(respostas.has("200")).isTrue();
            verificarErroDocumentado(respostas, "400");
            verificarErroDocumentado(respostas, "404");
        }
        JsonNode tipo = spec.path("components").path("schemas").path("CriarEstrategiaRequest")
                .path("properties").path("tipo");
        assertThat(tipo.path("type").asText()).isEqualTo("string");
        assertThat(tipo.path("enum").toString()).isEqualTo(
                "[\"CODE_SMELLS\",\"ACOPLAMENTO_COESAO_PRINCIPIOS\",\"PADROES_DE_PROJETO\"]");
    }

    private void verificarErroDocumentado(JsonNode respostas, String status) {
        assertThat(respostas.path(status).path("content").path("application/problem+json")
                .path("schema").path("$ref").asText()).isEqualTo("#/components/schemas/ProblemDetail");
    }

    private void verificarRejeicaoSemAlterarBanco(String corpo) throws Exception {
        verificarProblemDetail(mockMvc.perform(post("/api/v1/estrategias")
                .contentType(MediaType.APPLICATION_JSON).content(corpo)));
        verificarProblemDetail(mockMvc.perform(put("/api/v1/estrategias/{id}", original.getId())
                .contentType(MediaType.APPLICATION_JSON).content(corpo)));
        assertThat(repository.count()).isEqualTo(1);
        Estrategia persistida = repository.findById(original.getId()).orElseThrow();
        assertThat(persistida.getNome()).isEqualTo("Original");
        assertThat(persistida.getTipo()).isEqualTo(TipoEstrategia.CODE_SMELLS);
        assertThat(persistida.getDescricao()).isEqualTo("Descricao original");
        assertThat(persistida.getPrompt()).isEqualTo("Prompt original");
    }

    private void verificarProblemDetail(ResultActions resultado) throws Exception {
        resultado.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Requisicao invalida"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.instance").isNotEmpty());
    }

    private static ObjectNode corpoValido() {
        ObjectNode corpo = new ObjectMapper().createObjectNode();
        corpo.put("nome", "Novo nome");
        corpo.put("tipo", "PADROES_DE_PROJETO");
        corpo.put("descricao", "Nova descricao");
        corpo.put("prompt", "Novo prompt");
        return corpo;
    }
}
