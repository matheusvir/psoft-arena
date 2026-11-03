package br.edu.ufcg.psoft.arena.turma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class TurmaEstudanteOpenApiTest {

    private static final String COLECAO = "/api/v1/turmas/{id}/estudantes";
    private static final String ITEM = COLECAO + "/{estudanteId}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void apiDocsExpoeRotasEMetodosDaAssociacao() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['" + COLECAO + "'].post").exists())
                .andExpect(jsonPath("$.paths['" + COLECAO + "'].get").exists())
                .andExpect(jsonPath("$.paths['" + ITEM + "'].get").exists())
                .andExpect(jsonPath("$.paths['" + ITEM + "'].delete").exists());
    }

    @Test
    void postDocumenta201ComLocationEDto() throws Exception {
        JsonNode resposta = apiDocs().path("paths").path(COLECAO).path("post")
                .path("responses").path("201");

        assertThat(resposta.path("headers").path("Location").path("schema").path("type").asText())
                .isEqualTo("string");
        assertThat(resposta.path("headers").path("Location").path("schema").path("format").asText())
                .isEqualTo("uri");
        assertThat(resposta.path("content").path("application/json").path("schema").path("$ref").asText())
                .isEqualTo("#/components/schemas/TurmaEstudanteResponse");
    }

    @Test
    void operacoesDocumentamConjuntoExatoDeRespostas() throws Exception {
        JsonNode paths = apiDocs().path("paths");

        assertThat(codigos(paths.path(COLECAO).path("post")))
                .containsExactlyInAnyOrderElementsOf(Set.of("201", "400", "404", "409"));
        assertThat(codigos(paths.path(COLECAO).path("get")))
                .containsExactlyInAnyOrderElementsOf(Set.of("200", "404"));
        assertThat(codigos(paths.path(ITEM).path("get")))
                .containsExactlyInAnyOrderElementsOf(Set.of("200", "404"));
        assertThat(codigos(paths.path(ITEM).path("delete")))
                .containsExactlyInAnyOrderElementsOf(Set.of("204", "404", "409"));
    }

    @Test
    void errosDocumentamProblemDetailESucesso204NaoTemCorpo() throws Exception {
        JsonNode paths = apiDocs().path("paths");

        for (String codigo : List.of("400", "404", "409")) {
            assertProblemDetail(paths.path(COLECAO).path("post").path("responses").path(codigo));
        }
        for (String codigo : List.of("404")) {
            assertProblemDetail(paths.path(COLECAO).path("get").path("responses").path(codigo));
            assertProblemDetail(paths.path(ITEM).path("get").path("responses").path(codigo));
        }
        for (String codigo : List.of("404", "409")) {
            assertProblemDetail(paths.path(ITEM).path("delete").path("responses").path(codigo));
        }

        JsonNode resposta204 = paths.path(ITEM).path("delete").path("responses").path("204");
        assertThat(resposta204.has("content") && !resposta204.path("content").isEmpty()).isFalse();
    }

    private JsonNode apiDocs() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString());
    }

    private List<String> codigos(JsonNode operacao) {
        List<String> codigos = new ArrayList<>();
        operacao.path("responses").fieldNames().forEachRemaining(codigos::add);
        return codigos;
    }

    private void assertProblemDetail(JsonNode resposta) {
        JsonNode content = resposta.path("content");
        List<String> mediaTypes = new ArrayList<>();
        content.fieldNames().forEachRemaining(mediaTypes::add);

        assertThat(mediaTypes).containsExactly("application/problem+json");
        assertThat(content.path("application/problem+json").path("schema").path("$ref").asText())
                .isEqualTo("#/components/schemas/ProblemDetail");
    }
}
