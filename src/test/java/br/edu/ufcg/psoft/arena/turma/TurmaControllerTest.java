package br.edu.ufcg.psoft.arena.turma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SpringBootTest
@AutoConfigureMockMvc
class TurmaControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private JdbcTemplate jdbc;
    private long professorId;

    @BeforeEach
    void prepararBanco() throws Exception {
        jdbc.update("delete from turma_estudante");
        jdbc.update("delete from turma");
        jdbc.update("delete from professor");
        professorId = criarProfessor("ana@example.com");
    }

    @Test
    void cadastrarConsultarEListar() throws Exception {
        mvc.perform(get("/api/v1/turmas")).andExpect(status().isOk()).andExpect(content().json("[]"));
        long id = criarTurma(payload());
        mvc.perform(get("/api/v1/turmas/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Turma A"))
                .andExpect(jsonPath("$.codigo").value("PSOFT"))
                .andExpect(jsonPath("$.periodoLetivo").value("2026.2"))
                .andExpect(jsonPath("$.professorId").value(professorId))
                .andExpect(jsonPath("$.status").value("ATIVA"))
                .andExpect(jsonPath("$.professor").doesNotExist());
        mvc.perform(get("/api/v1/turmas")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(id));
    }

    @Test
    void professorInexistenteNaoCriaTurma() throws Exception {
        mvc.perform(post("/api/v1/turmas").contentType(MediaType.APPLICATION_JSON)
                        .content(payload().put("professorId", 999999L).toString()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(jdbc.queryForObject("select count(*) from turma", Long.class)).isZero();
    }

    @Test
    void duplicidadeDeCodigoEPeriodoNaoPersiste() throws Exception {
        criarTurma(payload());
        mvc.perform(post("/api/v1/turmas").contentType(MediaType.APPLICATION_JSON)
                        .content(payload().put("nome", "Duplicada").toString()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(jdbc.queryForObject("select count(*) from turma", Long.class)).isEqualTo(1L);
    }

    @Test
    void mesmoCodigoEmOutroPeriodoPermitido() throws Exception {
        criarTurma(payload());
        criarTurma(payload().put("periodoLetivo", "2027.1"));
        assertThat(jdbc.queryForObject("select count(*) from turma", Long.class)).isEqualTo(2L);
    }

    @Test
    void atualizarTodosOsCamposEMantemStatus() throws Exception {
        long id = criarTurma(payload());
        long outroProfessor = criarProfessor("outro@example.com");
        ObjectNode request = payload().put("nome", "Turma B").put("codigo", "NOVO")
                .put("periodoLetivo", "2027.1").put("professorId", outroProfessor).put("status", "ENCERRADA");
        mvc.perform(put("/api/v1/turmas/{id}", id).contentType(MediaType.APPLICATION_JSON).content(request.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Turma B"))
                .andExpect(jsonPath("$.codigo").value("NOVO")).andExpect(jsonPath("$.periodoLetivo").value("2027.1"))
                .andExpect(jsonPath("$.professorId").value(outroProfessor)).andExpect(jsonPath("$.status").value("ATIVA"));
        mvc.perform(get("/api/v1/turmas/{id}", id)).andExpect(jsonPath("$.codigo").value("NOVO"));
    }

    @Test
    void manterProprioCodigoEPeriodoNaoCausaConflito() throws Exception {
        long id = criarTurma(payload());
        mvc.perform(put("/api/v1/turmas/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(payload().toString())).andExpect(status().isOk());
    }

    @Test
    void professorInexistenteNaEdicaoNaoAlteraTurma() throws Exception {
        long id = criarTurma(payload());
        mvc.perform(put("/api/v1/turmas/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(payload().put("nome", "Nao deve salvar").put("professorId", 999999L).toString()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        assertTurmaOriginal(id);
    }

    @Test
    void duplicidadeNaEdicaoDesfazAlteracoes() throws Exception {
        criarTurma(payload());
        long id = criarTurma(payload().put("codigo", "OUTRO"));
        mvc.perform(put("/api/v1/turmas/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(payload().put("nome", "Nao deve salvar").toString()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        mvc.perform(get("/api/v1/turmas/{id}", id)).andExpect(jsonPath("$.codigo").value("OUTRO"))
                .andExpect(jsonPath("$.nome").value("Turma A"));
    }

    @Test
    void encerrarMantemHistoricoEEdicaoNaoReativa() throws Exception {
        long id = criarTurma(payload());
        mvc.perform(patch("/api/v1/turmas/{id}/encerrar", id)).andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mvc.perform(get("/api/v1/turmas/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENCERRADA"));
        mvc.perform(get("/api/v1/turmas")).andExpect(jsonPath("$[0].status").value("ENCERRADA"));
        mvc.perform(put("/api/v1/turmas/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(payload().put("nome", "Historico").put("status", "ATIVA").toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Historico"))
                .andExpect(jsonPath("$.status").value("ENCERRADA"));
        mvc.perform(get("/api/v1/turmas/{id}", id)).andExpect(jsonPath("$.status").value("ENCERRADA"));
        mvc.perform(patch("/api/v1/turmas/{id}/encerrar", id)).andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Turma ja encerrada."));
    }

    @Test
    void statusEnviadoNaCriacaoNaoMudaEstadoInicial() throws Exception {
        criarTurma(payload().put("status", "ENCERRADA"));
    }

    @Test
    void recursoInexistente() throws Exception {
        mvc.perform(get("/api/v1/turmas/999999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        mvc.perform(put("/api/v1/turmas/999999").contentType(MediaType.APPLICATION_JSON).content(payload().toString()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        mvc.perform(patch("/api/v1/turmas/999999/encerrar")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @ParameterizedTest
    @ValueSource(strings = {"nome", "codigo", "periodoLetivo", "professorId"})
    void campoObrigatorioAusente(String campo) throws Exception {
        ObjectNode request = payload();
        request.remove(campo);
        validarRejeicaoNaCriacaoEEdicao(request, campo);
    }

    @ParameterizedTest
    @ValueSource(strings = {"nome", "codigo", "periodoLetivo"})
    void campoEmBranco(String campo) throws Exception {
        validarRejeicaoNaCriacaoEEdicao(payload().put(campo, "   "), campo);
    }

    @ParameterizedTest
    @ValueSource(strings = {"nome", "codigo", "periodoLetivo"})
    void tamanhoExcedido(String campo) throws Exception {
        int limite = switch (campo) {
            case "nome" -> 120;
            case "codigo" -> 40;
            default -> 20;
        };
        validarRejeicaoNaCriacaoEEdicao(payload().put(campo, "a".repeat(limite + 1)), campo);
    }

    private void validarRejeicaoNaCriacaoEEdicao(ObjectNode request, String campo) throws Exception {
        mvc.perform(post("/api/v1/turmas").contentType(MediaType.APPLICATION_JSON).content(request.toString()))
                .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.erros." + campo).exists());
        assertThat(jdbc.queryForObject("select count(*) from turma", Long.class)).isZero();
        long id = criarTurma(payload());
        mvc.perform(put("/api/v1/turmas/{id}", id).contentType(MediaType.APPLICATION_JSON).content(request.toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros." + campo).exists());
        assertTurmaOriginal(id);
    }

    private void assertTurmaOriginal(long id) throws Exception {
        mvc.perform(get("/api/v1/turmas/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Turma A")).andExpect(jsonPath("$.codigo").value("PSOFT"))
                .andExpect(jsonPath("$.professorId").value(professorId)).andExpect(jsonPath("$.status").value("ATIVA"));
    }

    private long criarProfessor(String email) throws Exception {
        String body = mapper.createObjectNode().put("nome", "Professora Ana").put("email", email).toString();
        var resultado = mvc.perform(post("/api/v1/professores").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    private long criarTurma(ObjectNode request) throws Exception {
        var resultado = mvc.perform(post("/api/v1/turmas").contentType(MediaType.APPLICATION_JSON).content(request.toString()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ATIVA")).andReturn();
        long id = mapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
        assertThat(resultado.getResponse().getHeader("Location")).endsWith("/api/v1/turmas/" + id);
        return id;
    }

    private ObjectNode payload() {
        return mapper.createObjectNode().put("nome", "Turma A").put("codigo", "PSOFT")
                .put("periodoLetivo", "2026.2").put("professorId", professorId);
    }
}
