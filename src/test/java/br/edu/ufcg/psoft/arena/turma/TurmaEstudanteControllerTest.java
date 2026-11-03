package br.edu.ufcg.psoft.arena.turma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:turma-estudante-api;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class TurmaEstudanteControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private JdbcTemplate jdbc;

    private long professorId;

    @BeforeEach
    void prepararBanco() throws Exception {
        jdbc.update("delete from turma_estudante");
        jdbc.update("delete from turma");
        jdbc.update("delete from estudante");
        jdbc.update("delete from professor");
        professorId = criarProfessor("ana@example.com");
    }

    @Test
    void adicionaEstudanteComLocationEDto() throws Exception {
        long turmaId = criarTurma();
        long estudanteId = criarEstudante("Maria Silva", "123", "maria@example.com");

        MvcResult resultado = mvc.perform(post(rota(turmaId))
                        .contentType(MediaType.APPLICATION_JSON).content(payload(estudanteId)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        endsWith("/api/v1/turmas/" + turmaId + "/estudantes/" + estudanteId)))
                .andExpect(jsonPath("$.*", hasSize(4)))
                .andExpect(jsonPath("$.estudanteId").value(estudanteId))
                .andExpect(jsonPath("$.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.matricula").value("123"))
                .andExpect(jsonPath("$.associadoEm").isNotEmpty())
                .andReturn();

        assertThat(resultado.getResponse().getContentAsString()).doesNotContain("turma", "email");
        assertThat(jdbc.queryForObject("select count(*) from turma_estudante", Long.class)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("select associado_em from turma_estudante", Object.class)).isNotNull();

        String localizacao = resultado.getResponse().getHeader("Location");
        assertThat(localizacao).isNotNull();
        mvc.perform(get(localizacao)).andExpect(status().isOk())
                .andExpect(jsonPath("$.estudanteId").value(estudanteId))
                .andExpect(jsonPath("$.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.matricula").value("123"))
                .andExpect(jsonPath("$.associadoEm").isNotEmpty());
    }

    @Test
    void listaVaziaQuandoTurmaSemVinculos() throws Exception {
        long turmaId = criarTurma();
        mvc.perform(get(rota(turmaId))).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void listaEstudantesDaTurmaSemDependerDaOrdem() throws Exception {
        long turmaId = criarTurma();
        long primeira = criarEstudante("Ana", "111", "ana@example.com");
        long segunda = criarEstudante("Bia", "222", "bia@example.com");
        adicionar(turmaId, primeira).andExpect(status().isCreated());
        adicionar(turmaId, segunda).andExpect(status().isCreated());

        mvc.perform(get(rota(turmaId))).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.estudanteId == " + primeira + ")].nome").value("Ana"))
                .andExpect(jsonPath("$[?(@.estudanteId == " + segunda + ")].nome").value("Bia"));
    }

    @Test
    void mesmoEstudantePodeParticiparDeTurmasDiferentesESaidaDeUmaNaoAfetaOutra() throws Exception {
        long primeiraTurma = criarTurma("PSOFT-A", "2026.2");
        long segundaTurma = criarTurma("PSOFT-B", "2026.2");
        long estudanteId = criarEstudante("Ana", "111", "ana@example.com");

        adicionar(primeiraTurma, estudanteId).andExpect(status().isCreated());
        adicionar(segundaTurma, estudanteId).andExpect(status().isCreated());

        mvc.perform(get(rota(primeiraTurma))).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get(rota(segundaTurma))).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));

        mvc.perform(delete(rota(primeiraTurma) + "/" + estudanteId)).andExpect(status().isNoContent());

        mvc.perform(get(rota(primeiraTurma))).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get(rota(segundaTurma))).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].estudanteId").value(estudanteId));
    }

    @Test
    void removeVinculoERetorna204() throws Exception {
        long turmaId = criarTurma();
        long estudanteId = criarEstudante("Ana", "111", "ana@example.com");
        adicionar(turmaId, estudanteId).andExpect(status().isCreated());

        mvc.perform(delete(rota(turmaId) + "/" + estudanteId)).andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mvc.perform(get(rota(turmaId))).andExpect(status().isOk()).andExpect(content().json("[]"));
        assertThat(jdbc.queryForObject("select count(*) from turma_estudante", Long.class)).isZero();
        mvc.perform(get("/api/v1/estudantes/{id}", estudanteId)).andExpect(status().isOk());
    }

    @Test
    void corpoInvalidoRetorna400SemCriarVinculo() throws Exception {
        long turmaId = criarTurma();

        mvc.perform(post(rota(turmaId)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erros.estudanteId").exists());
        mvc.perform(post(rota(turmaId)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estudanteId\":null}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.estudanteId").exists());
        mvc.perform(post(rota(turmaId)).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        assertThat(jdbc.queryForObject("select count(*) from turma_estudante", Long.class)).isZero();
        mvc.perform(get(rota(turmaId))).andExpect(content().json("[]"));
    }

    @Test
    void turmaInexistenteRetorna404NosEndpoints() throws Exception {
        long estudanteId = criarEstudante("Ana", "111", "ana@example.com");
        long turmaInexistente = 999999L;

        mvc.perform(post(rota(turmaInexistente)).contentType(MediaType.APPLICATION_JSON).content(payload(estudanteId)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        mvc.perform(get(rota(turmaInexistente))).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        mvc.perform(get(rota(turmaInexistente) + "/" + estudanteId)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        mvc.perform(delete(rota(turmaInexistente) + "/" + estudanteId)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        assertThat(jdbc.queryForObject("select count(*) from turma_estudante", Long.class)).isZero();
    }

    @Test
    void estudanteInexistenteRetorna404SemCriarVinculo() throws Exception {
        long turmaId = criarTurma();
        mvc.perform(post(rota(turmaId)).contentType(MediaType.APPLICATION_JSON).content(payload(999999L)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
        mvc.perform(get(rota(turmaId))).andExpect(content().json("[]"));
    }

    @Test
    void vinculoInexistenteRetorna404NoDelete() throws Exception {
        long turmaId = criarTurma();
        long estudanteId = criarEstudante("Ana", "111", "ana@example.com");
        mvc.perform(delete(rota(turmaId) + "/" + estudanteId)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void consultaVinculoInexistenteRetorna404() throws Exception {
        long turmaId = criarTurma();
        long estudanteId = criarEstudante("Ana", "111", "ana@example.com");
        mvc.perform(get(rota(turmaId) + "/" + estudanteId)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void turmaEncerradaContinuaListandoEConsultandoVinculos() throws Exception {
        long turmaId = criarTurma();
        long estudanteId = criarEstudante("Ana", "111", "ana@example.com");
        adicionar(turmaId, estudanteId).andExpect(status().isCreated());
        encerrar(turmaId);

        mvc.perform(get(rota(turmaId))).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].estudanteId").value(estudanteId));
        mvc.perform(get(rota(turmaId) + "/" + estudanteId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.estudanteId").value(estudanteId));
    }

    @Test
    void turmaEncerradaTemPrecedenciaSobreEstudanteInexistente() throws Exception {
        long turmaId = criarTurma();
        encerrar(turmaId);

        mvc.perform(post(rota(turmaId)).contentType(MediaType.APPLICATION_JSON).content(payload(999999L)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409));
        assertThat(jdbc.queryForObject("select count(*) from turma_estudante", Long.class)).isZero();
    }

    @Test
    void turmaEncerradaTemPrecedenciaSobreVinculoInexistente() throws Exception {
        long turmaId = criarTurma();
        long estudanteId = criarEstudante("Ana", "111", "ana@example.com");
        encerrar(turmaId);

        mvc.perform(delete(rota(turmaId) + "/" + estudanteId)).andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409));
        assertThat(jdbc.queryForObject("select count(*) from turma_estudante", Long.class)).isZero();
    }

    @Test
    void turmaEncerradaImpedeAdicao() throws Exception {
        long turmaId = criarTurma();
        long estudanteId = criarEstudante("Ana", "111", "ana@example.com");
        encerrar(turmaId);

        mvc.perform(post(rota(turmaId)).contentType(MediaType.APPLICATION_JSON).content(payload(estudanteId)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409));
        assertThat(jdbc.queryForObject("select count(*) from turma_estudante", Long.class)).isZero();
    }

    @Test
    void turmaEncerradaImpedeRemocao() throws Exception {
        long turmaId = criarTurma();
        long estudanteId = criarEstudante("Ana", "111", "ana@example.com");
        adicionar(turmaId, estudanteId).andExpect(status().isCreated());
        encerrar(turmaId);

        mvc.perform(delete(rota(turmaId) + "/" + estudanteId)).andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409));
        mvc.perform(get(rota(turmaId))).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        assertThat(jdbc.queryForObject("select count(*) from turma_estudante", Long.class)).isEqualTo(1L);
    }

    @Test
    void duplicataRetorna409EMantemUmUnicoVinculo() throws Exception {
        long turmaId = criarTurma();
        long estudanteId = criarEstudante("Ana", "111", "ana@example.com");
        adicionar(turmaId, estudanteId).andExpect(status().isCreated());

        mvc.perform(post(rota(turmaId)).contentType(MediaType.APPLICATION_JSON).content(payload(estudanteId)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409));

        assertThat(jdbc.queryForObject("select count(*) from turma_estudante", Long.class)).isEqualTo(1L);
        mvc.perform(get(rota(turmaId))).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void postsConcorrentesPersistemUmVinculoERetornam201E409() throws Exception {
        long turmaId = criarTurma();
        long estudanteId = criarEstudante("Ana", "111", "ana@example.com");
        CountDownLatch requisicoesProntas = new CountDownLatch(2);
        CountDownLatch inicioSimultaneo = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> primeira = executor.submit(
                    () -> adicionarConcorrente(turmaId, estudanteId, requisicoesProntas, inicioSimultaneo));
            Future<Integer> segunda = executor.submit(
                    () -> adicionarConcorrente(turmaId, estudanteId, requisicoesProntas, inicioSimultaneo));
            try {
                assertThat(requisicoesProntas.await(10, TimeUnit.SECONDS)).isTrue();
            } finally {
                inicioSimultaneo.countDown();
            }
            Integer primeiroStatus = primeira.get(20, TimeUnit.SECONDS);
            Integer segundoStatus = segunda.get(20, TimeUnit.SECONDS);
            assertThat(new Integer[] {primeiroStatus, segundoStatus}).containsExactlyInAnyOrder(201, 409);
        }

        assertThat(jdbc.queryForObject("select count(*) from turma_estudante", Long.class)).isEqualTo(1L);
        mvc.perform(get(rota(turmaId))).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
    }

    private int adicionarConcorrente(long turmaId, long estudanteId, CountDownLatch requisicoesProntas,
            CountDownLatch inicioSimultaneo) throws Exception {
        requisicoesProntas.countDown();
        if (!inicioSimultaneo.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Inicio das requisicoes concorrentes nao foi liberado");
        }
        return adicionar(turmaId, estudanteId).andReturn().getResponse().getStatus();
    }

    private ResultActions adicionar(long turmaId, long estudanteId) throws Exception {
        return mvc.perform(post(rota(turmaId)).contentType(MediaType.APPLICATION_JSON).content(payload(estudanteId)));
    }

    private void encerrar(long turmaId) throws Exception {
        mvc.perform(patch("/api/v1/turmas/{id}/encerrar", turmaId)).andExpect(status().isNoContent());
    }

    private long criarProfessor(String email) throws Exception {
        String body = mapper.createObjectNode().put("nome", "Professora Ana").put("email", email).toString();
        MvcResult resultado = mvc.perform(post("/api/v1/professores")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    private long criarEstudante(String nome, String matricula, String email) throws Exception {
        ObjectNode body = mapper.createObjectNode().put("nome", nome).put("matricula", matricula).put("email", email);
        MvcResult resultado = mvc.perform(post("/api/v1/estudantes")
                        .contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    private long criarTurma() throws Exception {
        return criarTurma("PSOFT", "2026.2");
    }

    private long criarTurma(String codigo, String periodoLetivo) throws Exception {
        ObjectNode body = mapper.createObjectNode().put("nome", "Turma " + codigo)
                .put("codigo", codigo).put("periodoLetivo", periodoLetivo).put("professorId", professorId);
        MvcResult resultado = mvc.perform(post("/api/v1/turmas")
                        .contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    private String rota(long turmaId) {
        return "/api/v1/turmas/" + turmaId + "/estudantes";
    }

    private String payload(long estudanteId) {
        return mapper.createObjectNode().put("estudanteId", estudanteId).toString();
    }
}
