package br.edu.ufcg.psoft.arena.professor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class ProfessorControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void limparBanco() {
        jdbc.update("delete from turma_estudante");
        jdbc.update("delete from turma");
        jdbc.update("delete from professor");
    }

    @Test
    void cadastrarConsultarEListar() throws Exception {
        mvc.perform(get("/api/v1/professores")).andExpect(status().isOk()).andExpect(content().json("[]"));

        var resultado = mvc.perform(post("/api/v1/professores").contentType(MediaType.APPLICATION_JSON)
                        .content(payload().toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Ana"))
                .andExpect(jsonPath("$.email").value("ana@example.com"))
                .andReturn();
        long id = mapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
        assertThat(resultado.getResponse().getHeader("Location")).endsWith("/api/v1/professores/" + id);

        mvc.perform(get("/api/v1/professores/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id)).andExpect(jsonPath("$.nome").value("Ana"));
        mvc.perform(get("/api/v1/professores")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(id));
    }

    @Test
    void emailDuplicadoNaoPersiste() throws Exception {
        mvc.perform(post("/api/v1/professores").contentType(MediaType.APPLICATION_JSON)
                .content(payload().toString())).andExpect(status().isCreated());
        mvc.perform(post("/api/v1/professores").contentType(MediaType.APPLICATION_JSON)
                        .content(payload().put("nome", "Outra Ana").toString()))
                .andExpect(status().isConflict()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409));
        assertThat(jdbc.queryForObject("select count(*) from professor", Long.class)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("select nome from professor", String.class)).isEqualTo("Ana");
    }

    @Test
    void professorInexistente() throws Exception {
        mvc.perform(get("/api/v1/professores/999999"))
                .andExpect(status().isNotFound()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @ParameterizedTest
    @ValueSource(strings = {"nome", "email"})
    void campoObrigatorioAusente(String campo) throws Exception {
        ObjectNode request = payload();
        request.remove(campo);
        validarRejeicao(request, campo);
    }

    @ParameterizedTest
    @ValueSource(strings = {"nome", "email"})
    void campoEmBranco(String campo) throws Exception {
        validarRejeicao(payload().put(campo, "   "), campo);
    }

    @ParameterizedTest
    @ValueSource(strings = {"nome", "email"})
    void tamanhoExcedido(String campo) throws Exception {
        String valor = campo.equals("nome") ? "a".repeat(121) : "a".repeat(150) + "@example.com";
        validarRejeicao(payload().put(campo, valor), campo);
    }

    @Test
    void emailInvalido() throws Exception {
        validarRejeicao(payload().put("email", "email-invalido"), "email");
    }

    private void validarRejeicao(ObjectNode request, String campo) throws Exception {
        mvc.perform(post("/api/v1/professores").contentType(MediaType.APPLICATION_JSON).content(request.toString()))
                .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.erros." + campo).exists());
        assertThat(jdbc.queryForObject("select count(*) from professor", Long.class)).isZero();
    }

    private ObjectNode payload() {
        return mapper.createObjectNode().put("nome", "Ana").put("email", "ana@example.com");
    }
}
