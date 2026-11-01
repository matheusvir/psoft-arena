package br.edu.ufcg.psoft.arena.problema;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProblemaOpenApiTest {

    @Autowired
    private MockMvc mockMvc;

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
}
