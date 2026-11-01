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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

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
        MvcResult resultado = cadastrar("  Maria Silva  ", " Ab123 ", " Maria@EXAMPLE.com ")
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.*", hasSize(4)))
                .andExpect(jsonPath("$.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.matricula").value("Ab123"))
                .andExpect(jsonPath("$.email").value("maria@example.com")).andReturn();
        MockHttpServletResponse resposta = resultado.getResponse();
        JsonNode estudante = objectMapper.readTree(resposta.getContentAsString());
        String localizacao = resposta.getHeader("Location");
        String estudanteJson = estudante.toString();
        mockMvc.perform(get(localizacao))
                .andExpect(status().isOk()).andExpect(content().json(estudanteJson, JsonCompareMode.STRICT));
        long estudanteId = estudante.get("id").asLong();
        Estudante persistido = estudanteRepository.findById(estudanteId).orElseThrow();
        assertThat(persistido.getEmail()).isEqualTo("maria@example.com");
    }

    @Test
    void listaVazia() throws Exception {
        mockMvc.perform(get(ROTA)).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void listaEstudantesSemDependerDaOrdem() throws Exception {
        JsonNode primeiro = criarEstudante("Ana", "123", "ana@example.com");
        JsonNode segundo = criarEstudante("Bia", "456", "bia@example.com");
        String resposta = mockMvc.perform(get(ROTA)).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))).andReturn().getResponse().getContentAsString();
        JsonNode estudantes = objectMapper.readTree(resposta);
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
        JsonNode documento = consultarOpenApi();
        JsonNode caminhos = documento.path("paths");
        JsonNode cadastro = caminhos.path(ROTA).path("post");
        List<String> respostasCadastro = codigosDeResposta(cadastro);
        boolean corpoObrigatorio = cadastro.path("requestBody").path("required").asBoolean();
        JsonNode listagem = caminhos.path(ROTA).path("get");
        JsonNode consulta = caminhos.path(ROTA + "/{id}").path("get");
        List<String> respostasListagem = codigosDeResposta(listagem);
        List<String> respostasConsulta = codigosDeResposta(consulta);
        assertThat(respostasCadastro).contains("201", "400", "409");
        assertThat(corpoObrigatorio).isTrue();
        assertThat(respostasListagem).contains("200");
        assertThat(respostasConsulta).contains("200", "404");
        JsonNode entrada = documento.path("components").path("schemas").path("EstudanteRequestDTO");
        List<String> camposObrigatorios = entrada.path("required").valueStream().map(JsonNode::asText).toList();
        assertThat(camposObrigatorios).containsExactlyInAnyOrder("nome", "matricula", "email");
        JsonNode campos = entrada.path("properties");
        int limiteNome = campos.path("nome").path("maxLength").asInt();
        int limiteMatricula = campos.path("matricula").path("maxLength").asInt();
        int limiteEmail = campos.path("email").path("maxLength").asInt();
        assertThat(limiteNome).isEqualTo(120);
        assertThat(limiteMatricula).isEqualTo(20);
        assertThat(limiteEmail).isEqualTo(160);
        JsonNode saida = documento.path("components").path("schemas").path("EstudanteResponseDTO").path("properties");
        List<String> camposDeSaida = saida.propertyStream().map(Map.Entry::getKey).toList();
        assertThat(camposDeSaida).containsExactlyInAnyOrder("id", "nome", "matricula", "email");
    }

    private JsonNode consultarOpenApi() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        String resposta = resultado.getResponse().getContentAsString();
        return objectMapper.readTree(resposta);
    }

    @Test
    void documentaEdicaoCompletaESeusErrosNoOpenApi() throws Exception {
        JsonNode documento = consultarOpenApi();
        JsonNode edicao = documento.path("paths").path(ROTA + "/{id}").path("put");
        List<String> respostasEdicao = codigosDeResposta(edicao);
        assertThat(respostasEdicao).contains("200", "400", "404", "409");
        JsonNode corpo = edicao.path("requestBody");
        assertThat(corpo.path("required").asBoolean()).isTrue();
        String schema = corpo.path("content").path("application/json").path("schema").path("$ref").asText();
        assertThat(schema).endsWith("/EstudanteRequestDTO");
        for (String codigo : new String[] {"400", "404", "409"}) {
            JsonNode resposta = edicao.path("responses").path(codigo).path("content");
            assertThat(resposta.has("application/problem+json")).isTrue();
        }
    }

    @ParameterizedTest(name = "{0}: {1}, ausente={2}")
    @MethodSource("camposInvalidos")
    void rejeitaCadastroInvalidoSemInserir(String campo, String valor, boolean ausente) throws Exception {
        ObjectNode dados = dadosComCampoInvalido(campo, valor, ausente);
        MockHttpServletRequestBuilder requisicao = post(ROTA).contentType(MediaType.APPLICATION_JSON)
                .content(dados.toString());
        ResultActions resultado = mockMvc.perform(requisicao);
        verificarErroDeValidacao(resultado, campo);
        assertThat(estudanteRepository.count()).isZero();
    }

    static Stream<Arguments> camposInvalidos() {
        Stream<Arguments> obrigatorios = Stream.of("nome", "matricula", "email").flatMap(campo -> Stream.of(
                Arguments.of(campo, null, true), Arguments.of(campo, null, false),
                Arguments.of(campo, "", false), Arguments.of(campo, "   ", false)));
        Stream<Arguments> limitesEFormato = Stream.of(
                Arguments.of("nome", "a".repeat(121), false),
                Arguments.of("matricula", "a".repeat(21), false),
                Arguments.of("email", emailComTamanho(161), false),
                Arguments.of("email", "invalido", false));
        return Stream.concat(obrigatorios, limitesEFormato);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void aceitaLimitesDepoisDeNormalizar(boolean edicao) throws Exception {
        String nome = "A".repeat(120);
        String matricula = "Ab".repeat(10);
        String email = emailComTamanho(160);
        JsonNode estudante = gravarEstudante(edicao, " " + nome + " ", " " + matricula + " ", " " + email + " ");
        assertThat(estudante.get("nome").asText()).isEqualTo(nome);
        assertThat(estudante.get("matricula").asText()).isEqualTo(matricula);
        assertThat(estudante.get("email").asText()).isEqualTo(email);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void preservaEspacosInternos(boolean edicao) throws Exception {
        JsonNode estudante = gravarEstudante(edicao, " Maria  Silva ", " Ab 123 ", " MARIA@example.com ");
        assertThat(estudante.get("nome").asText()).isEqualTo("Maria  Silva");
        assertThat(estudante.get("matricula").asText()).isEqualTo("Ab 123");
    }

    @ParameterizedTest(name = "{0}: {1}, ausente={2}")
    @MethodSource("camposInvalidos")
    void rejeitaEdicaoInvalidaPreservandoDados(String campo, String valor, boolean ausente) throws Exception {
        JsonNode original = criarEstudante("Ana", "123", "ana@example.com");
        long estudanteId = original.get("id").asLong();
        ObjectNode dados = dadosComCampoInvalido(campo, valor, ausente);
        ResultActions resultado = editar(estudanteId, dados);
        verificarErroDeValidacao(resultado, campo);
        verificarConsulta(original);
        assertThat(estudanteRepository.count()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"matricula", "email"})
    void conflitoNaEdicaoReverteTodosOsCampos(String campoConflitante) throws Exception {
        JsonNode original = criarEstudante("Ana", "123", "ana@example.com");
        JsonNode outro = criarEstudante("Bia", "456", "bia@example.com");
        long estudanteId = original.get("id").asLong();
        ObjectNode dados = dadosValidos().put("nome", "Nome alterado").put("matricula", "789")
                .put("email", "livre@example.com");
        String valorConflitante = outro.get(campoConflitante).asText();
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
        ObjectNode dados = dadosValidos();
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
        JsonNode original = criarEstudante("Ana", "123", "ana@example.com");
        long estudanteId = original.get("id").asLong();
        ObjectNode dados = montarDados(nome, matricula, email);
        MvcResult resultado = editar(estudanteId, dados).andExpect(status().isOk()).andReturn();
        String resposta = resultado.getResponse().getContentAsString();
        JsonNode atualizado = objectMapper.readTree(resposta);
        verificarConsulta(atualizado);
        return atualizado;
    }

    @Test
    void editaTodosOsCamposPreservandoId() throws Exception {
        JsonNode original = criarEstudante("Ana", "123", "ana@example.com");
        long estudanteId = original.get("id").asLong();
        ObjectNode novosDados = montarDados("  Maria Silva ", " 456 ", " MARIA@EXAMPLE.com ");
        MvcResult resultado = editar(estudanteId, novosDados).andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(4))).andExpect(jsonPath("$.id").value(estudanteId))
                .andExpect(jsonPath("$.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.matricula").value("456"))
                .andExpect(jsonPath("$.email").value("maria@example.com")).andReturn();
        JsonNode atualizado = objectMapper.readTree(resultado.getResponse().getContentAsString());
        verificarConsulta(atualizado);
        assertThat(estudanteRepository.count()).isEqualTo(1);
    }

    @Test
    void permiteManterPropriosIdentificadores() throws Exception {
        JsonNode original = criarEstudante("Ana", "123", "ana@example.com");
        long estudanteId = original.get("id").asLong();
        ObjectNode novosDados = dadosValidos().put("matricula", " 123 ").put("email", " ANA@EXAMPLE.com ");
        editar(estudanteId, novosDados).andExpect(status().isOk());
        verificarConsulta(original);
    }

    @Test
    void editarInexistenteNaoCriaEstudante() throws Exception {
        ObjectNode dados = dadosValidos();
        editar(999999L, dados).andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
        assertThat(estudanteRepository.count()).isZero();
    }

    private ResultActions editar(long estudanteId, ObjectNode dados) throws Exception {
        MockHttpServletRequestBuilder requisicao = put(ROTA + "/" + estudanteId).contentType(MediaType.APPLICATION_JSON)
                .content(dados.toString());
        return mockMvc.perform(requisicao);
    }

    @ParameterizedTest
    @MethodSource("identificadoresDuplicados")
    void rejeitaDuplicataSemAlterarOriginal(String matricula, String email) throws Exception {
        JsonNode original = criarEstudante("Ana", "123", "ana@example.com");
        cadastrar("Outra", matricula, email).andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409)).andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.detail").isNotEmpty());
        assertThat(estudanteRepository.count()).isEqualTo(1);
        verificarConsulta(original);
    }

    @Test
    void conflitoDizQualIdentificadorColidiu() throws Exception {
        criarEstudante("Ana", "123", "ana@example.com");
        cadastrar("Outra", "123", "outra@example.com").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Matricula ja cadastrada."));
        cadastrar("Outra", "456", "ana@example.com").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Email ja cadastrado."));
        assertThat(estudanteRepository.count()).isEqualTo(1);
    }

    static Stream<Arguments> identificadoresDuplicados() {
        return Stream.of(Arguments.of(" 123 ", "outra@example.com"),
                Arguments.of("456", " Ana@EXAMPLE.com "));
    }

    @ParameterizedTest
    @MethodSource("identificadoresDuplicados")
    void concorrenciaPersisteSomenteUmEstudante(String matricula, String email) throws Exception {
        CountDownLatch requisicoesProntas = new CountDownLatch(2);
        CountDownLatch inicioSimultaneo = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> primeiraResposta = executor.submit(() -> cadastrarConcorrente(
                    "123", "ana@example.com", requisicoesProntas, inicioSimultaneo));
            Future<Integer> segundaResposta = executor.submit(() -> cadastrarConcorrente(
                    matricula, email, requisicoesProntas, inicioSimultaneo));
            try {
                assertThat(requisicoesProntas.await(10, TimeUnit.SECONDS)).isTrue();
            } finally {
                inicioSimultaneo.countDown();
            }
            Integer primeiroStatus = primeiraResposta.get(20, TimeUnit.SECONDS);
            Integer segundoStatus = segundaResposta.get(20, TimeUnit.SECONDS);
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
        MvcResult resultado = cadastrar("Ana", matricula, email).andReturn();
        return resultado.getResponse().getStatus();
    }

    private void verificarConsulta(JsonNode esperado) throws Exception {
        long estudanteId = esperado.get("id").asLong();
        String representacaoEsperada = esperado.toString();
        mockMvc.perform(get(ROTA + "/" + estudanteId)).andExpect(status().isOk())
                .andExpect(content().json(representacaoEsperada, JsonCompareMode.STRICT));
    }

    private static String emailComTamanho(int tamanho) {
        String inicio = "a".repeat(64) + "@" + "b".repeat(63) + ".";
        return inicio + "c".repeat(tamanho - inicio.length());
    }

    private ObjectNode dadosValidos() {
        return montarDados("Ana", "123", "ana@example.com");
    }

    private JsonNode criarEstudante(String nome, String matricula, String email) throws Exception {
        String resposta = cadastrar(nome, matricula, email).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resposta);
    }

    private ResultActions cadastrar(String nome, String matricula, String email) throws Exception {
        ObjectNode dados = montarDados(nome, matricula, email);
        MockHttpServletRequestBuilder requisicao = post(ROTA).contentType(MediaType.APPLICATION_JSON)
                .content(dados.toString());
        return mockMvc.perform(requisicao);
    }

    private ObjectNode montarDados(String nome, String matricula, String email) {
        return objectMapper.createObjectNode().put("nome", nome).put("matricula", matricula).put("email", email);
    }

    private List<String> codigosDeResposta(JsonNode operacao) {
        return operacao.path("responses").propertyStream().map(Map.Entry::getKey).toList();
    }
}
