package br.edu.ufcg.psoft.arena.estudante;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:estudante-api;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class EstudanteApiTest {

    private static final String ROTA = "/api/v1/estudantes";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EstudanteRepository estudanteRepository;

    @BeforeEach
    void limparEstudantes() {
        estudanteRepository.deleteAll();
    }

    @Test
    void criaEstudanteNormalizadoERetornaLocalizacaoConsultavel() throws Exception {
        var resultado = cadastrar("  Maria Silva  ", " Ab123 ", " Maria@EXAMPLE.com ")
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.*", hasSize(4)))
                .andExpect(jsonPath("$.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.matricula").value("Ab123"))
                .andExpect(jsonPath("$.email").value("maria@example.com")).andReturn();
        var resposta = resultado.getResponse();
        var estudante = objectMapper.readTree(resposta.getContentAsString());
        var localizacao = resposta.getHeader("Location");
        var estudanteJson = estudante.toString();
        mockMvc.perform(get(localizacao))
                .andExpect(status().isOk()).andExpect(content().json(estudanteJson, JsonCompareMode.STRICT));
        var estudanteId = estudante.get("id").asLong();
        var persistido = estudanteRepository.findById(estudanteId).orElseThrow();
        assertThat(persistido.getEmail()).isEqualTo("maria@example.com");
    }

    @Test
    void listaVazia() throws Exception {
        mockMvc.perform(get(ROTA)).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void listaEstudantesSemDependerDaOrdem() throws Exception {
        var primeiro = criarEstudante("Ana", "123", "ana@example.com");
        var segundo = criarEstudante("Bia", "456", "bia@example.com");
        var resposta = mockMvc.perform(get(ROTA)).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))).andReturn().getResponse().getContentAsString();
        var estudantes = objectMapper.readTree(resposta);
        assertThat(estudantes).containsExactlyInAnyOrder(primeiro, segundo);
    }

    @Test
    void consultaInexistente() throws Exception {
        mockMvc.perform(get(ROTA + "/999999")).andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404)).andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    void documentaCadastroEConsultasNoOpenApi() throws Exception {
        var documento = consultarOpenApi();
        var caminhos = documento.path("paths");
        var cadastro = caminhos.path(ROTA).path("post");
        var respostasCadastro = codigosDeResposta(cadastro);
        var corpoObrigatorio = cadastro.path("requestBody").path("required").asBoolean();
        var listagem = caminhos.path(ROTA).path("get");
        var consulta = caminhos.path(ROTA + "/{id}").path("get");
        var respostasListagem = codigosDeResposta(listagem);
        var respostasConsulta = codigosDeResposta(consulta);
        assertThat(respostasCadastro).contains("201", "400", "409");
        assertThat(corpoObrigatorio).isTrue();
        assertThat(respostasListagem).contains("200");
        assertThat(respostasConsulta).contains("200", "404");
        var entrada = documento.path("components").path("schemas").path("EstudanteRequestDTO");
        var camposObrigatorios = entrada.path("required").valueStream().map(JsonNode::asText).toList();
        assertThat(camposObrigatorios).containsExactlyInAnyOrder("nome", "matricula", "email");
        var campos = entrada.path("properties");
        var limiteNome = campos.path("nome").path("maxLength").asInt();
        var limiteMatricula = campos.path("matricula").path("maxLength").asInt();
        var limiteEmail = campos.path("email").path("maxLength").asInt();
        assertThat(limiteNome).isEqualTo(120);
        assertThat(limiteMatricula).isEqualTo(20);
        assertThat(limiteEmail).isEqualTo(160);
        var saida = documento.path("components").path("schemas").path("EstudanteResponseDTO").path("properties");
        var camposDeSaida = saida.propertyStream().map(Map.Entry::getKey).toList();
        assertThat(camposDeSaida).containsExactlyInAnyOrder("id", "nome", "matricula", "email");
    }

    private JsonNode consultarOpenApi() throws Exception {
        var resultado = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        var resposta = resultado.getResponse().getContentAsString();
        return objectMapper.readTree(resposta);
    }

    @Test
    void documentaEdicaoCompletaESeusErrosNoOpenApi() throws Exception {
        var documento = consultarOpenApi();
        var edicao = documento.path("paths").path(ROTA + "/{id}").path("put");
        var respostasEdicao = codigosDeResposta(edicao);
        assertThat(respostasEdicao).contains("200", "400", "404", "409");
        var corpo = edicao.path("requestBody");
        assertThat(corpo.path("required").asBoolean()).isTrue();
        var schema = corpo.path("content").path("application/json").path("schema").path("$ref").asText();
        assertThat(schema).endsWith("/EstudanteRequestDTO");
        for (var codigo : new String[] {"400", "404", "409"}) {
            var resposta = edicao.path("responses").path(codigo).path("content");
            assertThat(resposta.has("application/problem+json")).isTrue();
        }
    }

    @ParameterizedTest(name = "{0}: {1}, ausente={2}")
    @MethodSource("camposInvalidos")
    void rejeitaCadastroInvalidoSemInserir(String campo, String valor, boolean ausente) throws Exception {
        var dados = dadosComCampoInvalido(campo, valor, ausente);
        var requisicao = post(ROTA).contentType(MediaType.APPLICATION_JSON).content(dados.toString());
        var resultado = mockMvc.perform(requisicao);
        verificarErroDeValidacao(resultado, campo);
        assertThat(estudanteRepository.count()).isZero();
    }

    static Stream<Arguments> camposInvalidos() {
        var obrigatorios = Stream.of("nome", "matricula", "email").flatMap(campo -> Stream.of(
                Arguments.of(campo, null, true), Arguments.of(campo, null, false),
                Arguments.of(campo, "", false), Arguments.of(campo, "   ", false)));
        var limitesEFormato = Stream.of(
                Arguments.of("nome", "a".repeat(121), false),
                Arguments.of("matricula", "a".repeat(21), false),
                Arguments.of("email", emailComTamanho(161), false),
                Arguments.of("email", "invalido", false));
        return Stream.concat(obrigatorios, limitesEFormato);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void aceitaLimitesDepoisDeNormalizar(boolean edicao) throws Exception {
        var nome = "A".repeat(120);
        var matricula = "Ab".repeat(10);
        var email = emailComTamanho(160);
        var estudante = gravarEstudante(edicao, " " + nome + " ", " " + matricula + " ", " " + email + " ");
        assertThat(estudante.get("nome").asText()).isEqualTo(nome);
        assertThat(estudante.get("matricula").asText()).isEqualTo(matricula);
        assertThat(estudante.get("email").asText()).isEqualTo(email);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void preservaEspacosInternos(boolean edicao) throws Exception {
        var estudante = gravarEstudante(edicao, " Maria  Silva ", " Ab 123 ", " MARIA@example.com ");
        assertThat(estudante.get("nome").asText()).isEqualTo("Maria  Silva");
        assertThat(estudante.get("matricula").asText()).isEqualTo("Ab 123");
    }

    @ParameterizedTest(name = "{0}: {1}, ausente={2}")
    @MethodSource("camposInvalidos")
    void rejeitaEdicaoInvalidaPreservandoDados(String campo, String valor, boolean ausente) throws Exception {
        var original = criarEstudante("Ana", "123", "ana@example.com");
        var estudanteId = original.get("id").asLong();
        var dados = dadosComCampoInvalido(campo, valor, ausente);
        var resultado = editar(estudanteId, dados);
        verificarErroDeValidacao(resultado, campo);
        verificarConsulta(original);
        assertThat(estudanteRepository.count()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"matricula", "email"})
    void conflitoNaEdicaoReverteTodosOsCampos(String campoConflitante) throws Exception {
        var original = criarEstudante("Ana", "123", "ana@example.com");
        var outro = criarEstudante("Bia", "456", "bia@example.com");
        var estudanteId = original.get("id").asLong();
        var dados = dadosValidos().put("nome", "Nome alterado").put("matricula", "789")
                .put("email", "livre@example.com");
        var valorConflitante = outro.get(campoConflitante).asText();
        dados.put(campoConflitante, " " + valorConflitante + " ");
        if (campoConflitante.equals("email")) {
            dados.put("email", " BIA@EXAMPLE.com ");
        }
        editar(estudanteId, dados).andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409));
        verificarConsulta(original);
        verificarConsulta(outro);
        assertThat(estudanteRepository.count()).isEqualTo(2);
        if (campoConflitante.equals("matricula")) {
            // O email livre enviado no PUT rejeitado nao foi reservado.
            criarEstudante("Terceira", "789", "livre@example.com");
        } else {
            // O email conflitante continua pertencendo a Bia; a nova matricula continua livre.
            cadastrar("Terceira", "789", " BIA@EXAMPLE.com ").andExpect(status().isConflict());
            criarEstudante("Terceira", "789", "livre@example.com");
        }
    }

    private ObjectNode dadosComCampoInvalido(String campo, String valor, boolean ausente) {
        var dados = dadosValidos();
        if (ausente) {
            dados.remove(campo);
        } else {
            dados.put(campo, valor);
        }
        return dados;
    }

    private void verificarErroDeValidacao(ResultActions resultado, String campo) throws Exception {
        resultado.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.detail").isNotEmpty()).andExpect(jsonPath("$.erros." + campo).isNotEmpty());
    }

    private JsonNode gravarEstudante(boolean edicao, String nome, String matricula, String email) throws Exception {
        if (!edicao) {
            return criarEstudante(nome, matricula, email);
        }
        var original = criarEstudante("Ana", "123", "ana@example.com");
        var estudanteId = original.get("id").asLong();
        var dados = montarDados(nome, matricula, email);
        var resultado = editar(estudanteId, dados).andExpect(status().isOk()).andReturn();
        var resposta = resultado.getResponse().getContentAsString();
        var atualizado = objectMapper.readTree(resposta);
        verificarConsulta(atualizado);
        return atualizado;
    }

    @Test
    void editaTodosOsCamposPreservandoId() throws Exception {
        var original = criarEstudante("Ana", "123", "ana@example.com");
        var estudanteId = original.get("id").asLong();
        var novosDados = montarDados("  Maria Silva ", " 456 ", " MARIA@EXAMPLE.com ");
        var resultado = editar(estudanteId, novosDados).andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(4))).andExpect(jsonPath("$.id").value(estudanteId))
                .andExpect(jsonPath("$.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.matricula").value("456"))
                .andExpect(jsonPath("$.email").value("maria@example.com")).andReturn();
        var atualizado = objectMapper.readTree(resultado.getResponse().getContentAsString());
        verificarConsulta(atualizado);
        assertThat(estudanteRepository.count()).isEqualTo(1);
    }

    @Test
    void permiteManterPropriosIdentificadores() throws Exception {
        var original = criarEstudante("Ana", "123", "ana@example.com");
        var estudanteId = original.get("id").asLong();
        var novosDados = dadosValidos().put("matricula", " 123 ").put("email", " ANA@EXAMPLE.com ");
        editar(estudanteId, novosDados).andExpect(status().isOk());
        verificarConsulta(original);
    }

    @Test
    void editarInexistenteNaoCriaEstudante() throws Exception {
        var dados = dadosValidos();
        editar(999999L, dados).andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
        assertThat(estudanteRepository.count()).isZero();
    }

    private ResultActions editar(long estudanteId, ObjectNode dados) throws Exception {
        var requisicao = put(ROTA + "/" + estudanteId).contentType(MediaType.APPLICATION_JSON)
                .content(dados.toString());
        return mockMvc.perform(requisicao);
    }

    @ParameterizedTest
    @MethodSource("identificadoresDuplicados")
    void rejeitaDuplicataSemAlterarOriginal(String matricula, String email) throws Exception {
        var original = criarEstudante("Ana", "123", "ana@example.com");
        cadastrar("Outra", matricula, email).andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409)).andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.detail").isNotEmpty());
        assertThat(estudanteRepository.count()).isEqualTo(1);
        verificarConsulta(original);
    }

    static Stream<Arguments> identificadoresDuplicados() {
        return Stream.of(Arguments.of(" 123 ", "outra@example.com"),
                Arguments.of("456", " Ana@EXAMPLE.com "));
    }

    @ParameterizedTest
    @MethodSource("identificadoresDuplicados")
    void concorrenciaPersisteSomenteUmEstudante(String matricula, String email) throws Exception {
        var requisicoesProntas = new CountDownLatch(2);
        var inicioSimultaneo = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var primeiraResposta = executor.submit(() -> cadastrarConcorrente(
                    "123", "ana@example.com", requisicoesProntas, inicioSimultaneo));
            var segundaResposta = executor.submit(() -> cadastrarConcorrente(
                    matricula, email, requisicoesProntas, inicioSimultaneo));
            try {
                assertThat(requisicoesProntas.await(10, TimeUnit.SECONDS)).isTrue();
            } finally {
                inicioSimultaneo.countDown();
            }
            var primeiroStatus = primeiraResposta.get(20, TimeUnit.SECONDS);
            var segundoStatus = segundaResposta.get(20, TimeUnit.SECONDS);
            assertThat(new Integer[] {primeiroStatus, segundoStatus}).containsExactlyInAnyOrder(201, 409);
        }
        assertThat(estudanteRepository.count()).isEqualTo(1);
    }

    private int cadastrarConcorrente(String matricula, String email, CountDownLatch requisicoesProntas,
            CountDownLatch inicioSimultaneo) throws Exception {
        requisicoesProntas.countDown();
        if (!inicioSimultaneo.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Inicio das requisicoes concorrentes nao foi liberado");
        }
        var resultado = cadastrar("Ana", matricula, email).andReturn();
        return resultado.getResponse().getStatus();
    }

    private void verificarConsulta(JsonNode esperado) throws Exception {
        var estudanteId = esperado.get("id").asLong();
        var representacaoEsperada = esperado.toString();
        mockMvc.perform(get(ROTA + "/" + estudanteId)).andExpect(status().isOk())
                .andExpect(content().json(representacaoEsperada, JsonCompareMode.STRICT));
    }

    private static String emailComTamanho(int tamanho) {
        var inicio = "a".repeat(64) + "@" + "b".repeat(63) + ".";
        return inicio + "c".repeat(tamanho - inicio.length());
    }

    private ObjectNode dadosValidos() {
        return montarDados("Ana", "123", "ana@example.com");
    }

    private JsonNode criarEstudante(String nome, String matricula, String email) throws Exception {
        var resposta = cadastrar(nome, matricula, email).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resposta);
    }

    private ResultActions cadastrar(String nome, String matricula, String email) throws Exception {
        var dados = montarDados(nome, matricula, email);
        var requisicao = post(ROTA).contentType(MediaType.APPLICATION_JSON).content(dados.toString());
        return mockMvc.perform(requisicao);
    }

    private ObjectNode montarDados(String nome, String matricula, String email) {
        return objectMapper.createObjectNode().put("nome", nome).put("matricula", matricula).put("email", email);
    }

    private List<String> codigosDeResposta(JsonNode operacao) {
        return operacao.path("responses").propertyStream().map(Map.Entry::getKey).toList();
    }
}
