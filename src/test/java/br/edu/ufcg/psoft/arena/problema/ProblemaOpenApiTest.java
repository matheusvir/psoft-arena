package br.edu.ufcg.psoft.arena.problema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ProblemaOpenApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void apiDocsExponeRotasEMetodosFixadosDeProblema() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/problemas'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/problemas'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/problemas/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/problemas/{id}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/problemas/{id}'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/v1/problemas/{id}/desativar'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/v1/atividades']").doesNotExist());
    }

    @Test
    void apiDocsSeparaCamposDeEntradaESaida() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.ProblemaRequest.properties.titulo.maxLength").value(200))
                .andExpect(jsonPath("$.components.schemas.ProblemaRequest.properties.descricao.maxLength").value(2000))
                .andExpect(jsonPath("$.components.schemas.ProblemaRequest.properties.requisitosFuncionais.maxLength").value(4000))
                .andExpect(jsonPath("$.components.schemas.ProblemaRequest.properties.requisitosProjeto.maxLength").value(4000))
                .andExpect(jsonPath("$.components.schemas.ProblemaRequest.properties.id").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.ProblemaRequest.properties.status").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.ProblemaResponse.properties.id").exists())
                .andExpect(jsonPath("$.components.schemas.ProblemaResponse.properties.status.enum")
                        .value(containsInAnyOrder("ATIVO", "INATIVO")))
                .andExpect(jsonPath("$.components.schemas.ProblemaResponse.properties.requisitosFuncionais").exists())
                .andExpect(jsonPath("$.components.schemas.ProblemaResponse.properties.requisitosProjeto").exists());
    }

    @ParameterizedTest(name = "{1} {0} documenta somente respostas {2}")
    @MethodSource("respostasPorOperacao")
    void operacoesDocumentamConjuntoExatoDeRespostas(
            String caminho,
            String metodo,
            Set<String> respostasEsperadas) throws Exception {
        JsonNode operacao = apiDocs().path("paths").path(caminho).path(metodo);
        JsonNode respostas = operacao.path("responses");
        List<String> codigos = new ArrayList<>();
        respostas.fieldNames().forEachRemaining(codigos::add);

        assertThat(codigos).containsExactlyInAnyOrderElementsOf(respostasEsperadas);
    }

    @Test
    void sucessoDocumentaLocationDtoArrayE204SemConteudo() throws Exception {
        JsonNode docs = apiDocs();
        JsonNode paths = docs.path("paths");

        JsonNode criar = paths.path("/api/v1/problemas").path("post");
        JsonNode respostaCriacao = criar.path("responses").path("201");
        assertThat(respostaCriacao.path("content").path("application/json")
                .path("schema").path("$ref").asText())
                .isEqualTo("#/components/schemas/ProblemaResponse");
        assertThat(respostaCriacao.path("headers").path("Location").path("schema").path("type").asText())
                .isEqualTo("string");
        assertThat(respostaCriacao.path("headers").path("Location").path("schema").path("format").asText())
                .isEqualTo("uri");
        assertThat(criar.path("requestBody").path("content").path("application/json")
                .path("schema").path("$ref").asText())
                .isEqualTo("#/components/schemas/ProblemaRequest");

        JsonNode lista = paths.path("/api/v1/problemas").path("get").path("responses").path("200")
                .path("content").path("application/json").path("schema");
        assertThat(lista.path("type").asText()).isEqualTo("array");
        assertThat(lista.path("items").path("$ref").asText())
                .isEqualTo("#/components/schemas/ProblemaResponse");

        assertDtoBody(paths.path("/api/v1/problemas/{id}").path("get").path("responses").path("200"));
        assertDtoBody(paths.path("/api/v1/problemas/{id}").path("put").path("responses").path("200"));
        assertSemConteudo(paths.path("/api/v1/problemas/{id}/desativar").path("patch").path("responses").path("204"));
        assertSemConteudo(paths.path("/api/v1/problemas/{id}").path("delete").path("responses").path("204"));
    }

    @ParameterizedTest(name = "{1} {0} {2} usa application/problem+json / ProblemDetail")
    @MethodSource("errosPorOperacao")
    void cadaErroDocumentaProblemDetail(String caminho, String metodo, String codigo) throws Exception {
        JsonNode resposta = apiDocs().path("paths").path(caminho).path(metodo)
                .path("responses").path(codigo);
        JsonNode content = resposta.path("content");
        List<String> mediaTypes = new ArrayList<>();
        content.fieldNames().forEachRemaining(mediaTypes::add);

        assertThat(mediaTypes).containsExactly("application/problem+json");
        assertThat(content.path("application/problem+json").path("schema").path("$ref").asText())
                .isEqualTo("#/components/schemas/ProblemDetail");
    }

    private JsonNode apiDocs() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString());
    }

    private void assertDtoBody(JsonNode resposta) {
        assertThat(resposta.path("content").path("application/json").path("schema").path("$ref").asText())
                .isEqualTo("#/components/schemas/ProblemaResponse");
    }

    private void assertSemConteudo(JsonNode resposta) {
        assertThat(resposta.has("content") && !resposta.path("content").isEmpty()).isFalse();
    }

    private static Stream<Arguments> respostasPorOperacao() {
        return Stream.of(
                Arguments.of("/api/v1/problemas", "post", Set.of("201", "400")),
                Arguments.of("/api/v1/problemas", "get", Set.of("200")),
                Arguments.of("/api/v1/problemas/{id}", "get", Set.of("200", "404")),
                Arguments.of("/api/v1/problemas/{id}", "put", Set.of("200", "400", "404")),
                Arguments.of("/api/v1/problemas/{id}/desativar", "patch", Set.of("204", "404")),
                Arguments.of("/api/v1/problemas/{id}", "delete", Set.of("204", "404", "409")));
    }

    private static Stream<Arguments> errosPorOperacao() {
        return Stream.of(
                Arguments.of("/api/v1/problemas", "post", "400"),
                Arguments.of("/api/v1/problemas/{id}", "get", "404"),
                Arguments.of("/api/v1/problemas/{id}", "put", "400"),
                Arguments.of("/api/v1/problemas/{id}", "put", "404"),
                Arguments.of("/api/v1/problemas/{id}/desativar", "patch", "404"),
                Arguments.of("/api/v1/problemas/{id}", "delete", "404"),
                Arguments.of("/api/v1/problemas/{id}", "delete", "409"));
    }
}
