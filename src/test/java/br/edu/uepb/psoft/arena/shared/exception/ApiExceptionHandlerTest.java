package br.edu.uepb.psoft.arena.shared.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

class ApiExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ControladorDeTeste())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void payloadInvalidoRetorna400ComProblemDetail() throws Exception {
        mockMvc.perform(post("/teste/validacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Requisicao invalida"))
                .andExpect(jsonPath("$.erros.nome").value("nome obrigatorio"));
    }

    @Test
    void violacaoDeIntegridadeRetorna409() throws Exception {
        mockMvc.perform(get("/teste/duplicado"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Operacao viola uma restricao de integridade dos dados."));
    }

    @Test
    void regraDeNegocioRetorna409ComMensagem() throws Exception {
        mockMvc.perform(get("/teste/regra"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Turma encerrada nao aceita associacoes."));
    }

    @Test
    void recursoInexistenteRetorna404ComMensagem() throws Exception {
        mockMvc.perform(get("/teste/inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Turma 99 nao encontrada."));
    }

    record RequisicaoDeTeste(@NotBlank(message = "nome obrigatorio") String nome) {
    }

    @RestController
    static class ControladorDeTeste {

        @PostMapping("/teste/validacao")
        void validar(@Valid @RequestBody RequisicaoDeTeste requisicao) {
        }

        @GetMapping("/teste/duplicado")
        void duplicado() {
            throw new DataIntegrityViolationException("matricula duplicada");
        }

        @GetMapping("/teste/regra")
        void regra() {
            throw new RegraDeNegocioException("Turma encerrada nao aceita associacoes.");
        }

        @GetMapping("/teste/inexistente")
        void inexistente() {
            throw new RecursoNaoEncontradoException("Turma 99 nao encontrada.");
        }
    }
}
