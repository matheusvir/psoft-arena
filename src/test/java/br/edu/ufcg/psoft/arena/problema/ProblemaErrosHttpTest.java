package br.edu.ufcg.psoft.arena.problema;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProblemaErrosHttpTest {

    private static final String CAMINHO_PROBLEMAS = "/api/v1/problemas";
    private static final String DETALHE_CORPO_INVALIDO = "O corpo da requisicao esta ausente ou malformado.";
    private static final String DETALHE_PARAMETRO_INVALIDO = "Um parametro da requisicao possui tipo invalido.";

    @LocalServerPort
    private int porta;

    private final HttpClient cliente = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Autowired
    private ObjectMapper objectMapper;

    private Long idParaLimpeza;

    @AfterEach
    void limparRegistroDeTeste() throws Exception {
        if (idParaLimpeza != null) {
            ResponseEntity<String> resposta = requisitar(
                    HttpMethod.DELETE, CAMINHO_PROBLEMAS + "/" + idParaLimpeza, null);
            assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            idParaLimpeza = null;
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("corposInvalidos")
    void corpoAusenteOuJsonMalformadoRetornaProblemDetailSemGravarOuAlterar(
            String descricao,
            HttpMethod metodo,
            String caminho,
            String corpo) throws Exception {
        RegistroProblema original = caminho.contains("{id}") ? criarProblema() : null;
        String caminhoResolvido = original == null
                ? caminho
                : caminho.replace("{id}", Long.toString(original.id()));
        int quantidadeAntes = original == null ? quantidadeProblemas() : -1;

        ResponseEntity<String> resposta = requisitar(metodo, caminhoResolvido, corpo);

        JsonNode problema = assertProblemDetail(
                resposta, HttpStatus.BAD_REQUEST, "Requisicao invalida", DETALHE_CORPO_INVALIDO,
                caminhoResolvido, Set.of("type", "title", "status", "detail", "instance"));
        assertThat(problema.has("erros")).isFalse();

        if (original == null) {
            assertThat(quantidadeProblemas()).isEqualTo(quantidadeAntes);
        } else {
            assertRegistro(original);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("idsDeRotaInvalidos")
    void idDeRotaNaoConversivelRetorna400ProblemDetailSemInternals(
            String descricao,
            HttpMethod metodo,
            String caminho,
            String corpo) throws Exception {
        ResponseEntity<String> resposta = requisitar(metodo, caminho, corpo);

        JsonNode problema = assertProblemDetail(
                resposta, HttpStatus.BAD_REQUEST, "Requisicao invalida", DETALHE_PARAMETRO_INVALIDO,
                caminho, Set.of("type", "title", "status", "detail", "instance"));
        assertThat(problema.has("erros")).isFalse();
    }

    @Test
    void validacaoDeCampoMantemErrosESemPersistir() throws Exception {
        int quantidadeAntes = quantidadeProblemas();
        ResponseEntity<String> respostaValidacao = requisitar(
                HttpMethod.POST,
                CAMINHO_PROBLEMAS,
                "{\"titulo\":\" \",\"descricao\":\"Descricao\","
                        + "\"requisitosFuncionais\":\"RF\",\"requisitosProjeto\":\"RP\"}");

        JsonNode problemaValidacao = assertProblemDetail(
                respostaValidacao, HttpStatus.BAD_REQUEST, "Requisicao invalida",
                "Um ou mais campos da requisicao sao invalidos.", CAMINHO_PROBLEMAS,
                Set.of("type", "title", "status", "detail", "instance", "erros"));
        assertThat(problemaValidacao.path("erros").path("titulo").asText()).isNotBlank();
        assertThat(quantidadeProblemas()).isEqualTo(quantidadeAntes);
    }

    @Test
    void idInteiroInexistenteMantem404ProblemDetail() throws Exception {
        String caminhoInexistente = CAMINHO_PROBLEMAS + "/-1";
        ResponseEntity<String> resposta404 = requisitar(HttpMethod.GET, caminhoInexistente, null);
        assertProblemDetail(
                resposta404, HttpStatus.NOT_FOUND, "Recurso nao encontrado",
                "Problema -1 não encontrado.", caminhoInexistente,
                Set.of("type", "title", "status", "detail", "instance"));
    }

    private JsonNode assertProblemDetail(
            ResponseEntity<String> resposta,
            HttpStatus status,
            String titulo,
            String detalhe,
            String instancia,
            Set<String> camposEsperados) throws Exception {
        assertThat(resposta.getStatusCode()).isEqualTo(status);
        MediaType tipoConteudo = resposta.getHeaders().getContentType();
        assertThat(tipoConteudo).isNotNull();
        assertThat(tipoConteudo.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)).isTrue();

        JsonNode problema = objectMapper.readTree(resposta.getBody());
        assertThat(problema.path("type").asText()).isEqualTo("about:blank");
        assertThat(problema.path("title").asText()).isEqualTo(titulo);
        assertThat(problema.path("status").asInt()).isEqualTo(status.value());
        assertThat(problema.path("detail").asText()).isEqualTo(detalhe);
        assertThat(problema.path("instance").asText()).isEqualTo(instancia);

        Set<String> campos = new HashSet<>();
        problema.fieldNames().forEachRemaining(campos::add);
        assertThat(campos).containsExactlyInAnyOrderElementsOf(camposEsperados);
        assertThat(resposta.getBody())
                .doesNotContain("timestamp", "HttpMessageNotReadableException",
                        "MethodArgumentTypeMismatchException", "stackTrace", "trace");
        return problema;
    }

    private ResponseEntity<String> requisitar(HttpMethod metodo, String caminho, String corpo)
            throws IOException, InterruptedException {
        HttpRequest.BodyPublisher body = corpo == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(corpo);
        HttpRequest requisicao = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + porta + caminho))
                .timeout(Duration.ofSeconds(10))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .method(metodo.name(), body)
                .build();
        HttpResponse<String> resposta = cliente.send(requisicao, HttpResponse.BodyHandlers.ofString());
        HttpHeaders cabecalhos = new HttpHeaders();
        resposta.headers().map().forEach((nome, valores) -> valores.forEach(valor -> cabecalhos.add(nome, valor)));
        return new ResponseEntity<>(resposta.body(), cabecalhos,
                HttpStatusCode.valueOf(resposta.statusCode()));
    }

    private RegistroProblema criarProblema() throws Exception {
        String titulo = "Registro original " + UUID.randomUUID();
        ObjectNode payload = objectMapper.createObjectNode()
                .put("titulo", titulo)
                .put("descricao", "Descricao original")
                .put("requisitosFuncionais", "RF original")
                .put("requisitosProjeto", "RP original");
        ResponseEntity<String> resposta = requisitar(
                HttpMethod.POST, CAMINHO_PROBLEMAS, objectMapper.writeValueAsString(payload));
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode corpo = objectMapper.readTree(resposta.getBody());
        RegistroProblema registro = new RegistroProblema(corpo.path("id").asLong(), titulo);
        idParaLimpeza = registro.id();
        return registro;
    }

    private void assertRegistro(RegistroProblema original) throws Exception {
        String caminho = CAMINHO_PROBLEMAS + "/" + original.id();
        ResponseEntity<String> resposta = requisitar(HttpMethod.GET, caminho, null);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode registro = objectMapper.readTree(resposta.getBody());
        assertThat(registro.path("titulo").asText()).isEqualTo(original.titulo());
        assertThat(registro.path("descricao").asText()).isEqualTo("Descricao original");
        assertThat(registro.path("requisitosFuncionais").asText()).isEqualTo("RF original");
        assertThat(registro.path("requisitosProjeto").asText()).isEqualTo("RP original");
        assertThat(registro.path("status").asText()).isEqualTo("ATIVO");
    }

    private int quantidadeProblemas() throws Exception {
        ResponseEntity<String> resposta = requisitar(HttpMethod.GET, CAMINHO_PROBLEMAS, null);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(resposta.getBody()).size();
    }

    private static Stream<Arguments> corposInvalidos() {
        return Stream.of(
                Arguments.of("POST com JSON malformado", HttpMethod.POST, CAMINHO_PROBLEMAS, "{\"titulo\":"),
                Arguments.of("PUT com JSON malformado", HttpMethod.PUT, CAMINHO_PROBLEMAS + "/{id}", "{\"titulo\":"),
                Arguments.of("POST sem corpo", HttpMethod.POST, CAMINHO_PROBLEMAS, null),
                Arguments.of("PUT sem corpo", HttpMethod.PUT, CAMINHO_PROBLEMAS + "/{id}", null));
    }

    private static Stream<Arguments> idsDeRotaInvalidos() {
        String jsonValido = "{\"titulo\":\"Titulo\",\"descricao\":\"Descricao\","
                + "\"requisitosFuncionais\":\"RF\",\"requisitosProjeto\":\"RP\"}";
        return Stream.of(
                Arguments.of("GET com id nao numerico", HttpMethod.GET, CAMINHO_PROBLEMAS + "/abc", null),
                Arguments.of("PUT com id nao numerico", HttpMethod.PUT, CAMINHO_PROBLEMAS + "/abc", jsonValido),
                Arguments.of("PATCH com id nao numerico", HttpMethod.PATCH,
                        CAMINHO_PROBLEMAS + "/abc/desativar", null),
                Arguments.of("DELETE com id nao numerico", HttpMethod.DELETE, CAMINHO_PROBLEMAS + "/abc", null));
    }

    private record RegistroProblema(long id, String titulo) {
    }
}
