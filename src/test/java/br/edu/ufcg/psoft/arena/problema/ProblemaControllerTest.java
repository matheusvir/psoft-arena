package br.edu.ufcg.psoft.arena.problema;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProblemaControllerTest {

    private static final Map<String, Integer> LIMITES = Map.of(
            "titulo", 200,
            "descricao", 2000,
            "requisitosFuncionais", 4000,
            "requisitosProjeto", 4000);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void criarRetorna201LocationCamposEStatusAtivoMesmoComStatusExtra() throws Exception {
        ObjectNode payload = payload("Título inicial")
                .put("status", "INATIVO");

        MvcResult resultado = mockMvc.perform(post("/api/v1/problemas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/problemas/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.titulo").value("Título inicial"))
                .andExpect(jsonPath("$.descricao").value("Descrição"))
                .andExpect(jsonPath("$.requisitosFuncionais").value("RF"))
                .andExpect(jsonPath("$.requisitosProjeto").value("RP"))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andReturn();

        Long id = objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(get("/api/v1/problemas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVO"));
    }

    @Test
    void listarIncluiProblemasAtivosEInativosEConsultarItem() throws Exception {
        Long ativo = criar("Ativo");
        Long inativo = criar("Inativo");
        mockMvc.perform(patch("/api/v1/problemas/{id}/desativar", inativo));

        mockMvc.perform(get("/api/v1/problemas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/api/v1/problemas/{id}", ativo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVO"));
        mockMvc.perform(get("/api/v1/problemas/{id}", inativo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INATIVO"));
    }

    @Test
    void putPreservaStatusAtivoEInativoMesmoComStatusExtra() throws Exception {
        Long ativo = criar("Ativo");
        Long inativo = criar("Inativo");
        mockMvc.perform(patch("/api/v1/problemas/{id}/desativar", inativo))
                .andExpect(status().isNoContent());

        mockMvc.perform(put("/api/v1/problemas/{id}", ativo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonComStatus("Título ativo editado", "INATIVO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.titulo").value("Título ativo editado"));

        mockMvc.perform(put("/api/v1/problemas/{id}", inativo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonComStatus("Título inativo editado", "ATIVO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INATIVO"))
                .andExpect(jsonPath("$.titulo").value("Título inativo editado"));
    }

    @Test
    void patchDesativarEhIdempotenteERetorna204SemCorpo() throws Exception {
        Long id = criar("Desativar");

        mockMvc.perform(patch("/api/v1/problemas/{id}/desativar", id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mockMvc.perform(patch("/api/v1/problemas/{id}/desativar", id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mockMvc.perform(get("/api/v1/problemas/{id}", id))
                .andExpect(jsonPath("$.status").value("INATIVO"));
    }

    @Test
    void deleteNoModeloAtualRemoveProblemaERetorna204SemCorpo() throws Exception {
        Long id = criar("Excluir no modelo sem atividades");

        mockMvc.perform(delete("/api/v1/problemas/{id}", id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mockMvc.perform(get("/api/v1/problemas/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void idsInexistentesRetornam404NasRotasDeItem() throws Exception {
        mockMvc.perform(get("/api/v1/problemas/{id}", -1))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/problemas/{id}", -1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonValido("Não existe")))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/v1/problemas/{id}/desativar", -1))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/problemas/{id}", -1))
                .andExpect(status().isNotFound());
    }

    @Test
    void postRejeitaCampoAusenteNuloEmBrancoOuExcedenteSemPersistir() throws Exception {
        for (String campo : LIMITES.keySet()) {
            for (String tipo : List.of("ausente", "nulo", "em branco", "excesso")) {
                ObjectNode payload = payload("POST inválido");
                aplicarInvalidade(payload, campo, tipo);

                mockMvc.perform(post("/api/v1/problemas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload)))
                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                        .andExpect(jsonPath("$.erros." + campo).exists());
            }
        }

        mockMvc.perform(get("/api/v1/problemas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void putRejeitaCampoAusenteNuloEmBrancoOuExcedenteSemAlterarRegistro() throws Exception {
        Long id = criar("Valor original");

        for (String campo : LIMITES.keySet()) {
            for (String tipo : List.of("ausente", "nulo", "em branco", "excesso")) {
                ObjectNode payload = payload("Tentativa inválida");
                aplicarInvalidade(payload, campo, tipo);

                mockMvc.perform(put("/api/v1/problemas/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload)))
                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                        .andExpect(jsonPath("$.erros." + campo).exists());
                mockMvc.perform(get("/api/v1/problemas/{id}", id))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.titulo").value("Valor original"))
                        .andExpect(jsonPath("$.descricao").value("Descrição"))
                        .andExpect(jsonPath("$.requisitosFuncionais").value("RF"))
                        .andExpect(jsonPath("$.requisitosProjeto").value("RP"))
                        .andExpect(jsonPath("$.status").value("ATIVO"));
            }
        }
    }

    @Test
    void roundTripHttpPreservaRequisitosComWhitespaceENovasLinhas() throws Exception {
        String requisitosFuncionais = "  RF inicial\nsegunda linha\r\nfinal  ";
        String requisitosProjeto = "  RP\tcom tab e espaços finais  ";
        ObjectNode payload = objectMapper.createObjectNode()
                .put("titulo", "  Título  ")
                .put("descricao", "  Descrição\n  ")
                .put("requisitosFuncionais", requisitosFuncionais)
                .put("requisitosProjeto", requisitosProjeto);

        MvcResult resultado = mockMvc.perform(post("/api/v1/problemas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        Long id = objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/v1/problemas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("  Título  "))
                .andExpect(jsonPath("$.descricao").value("  Descrição\n  "))
                .andExpect(jsonPath("$.requisitosFuncionais").value(requisitosFuncionais))
                .andExpect(jsonPath("$.requisitosProjeto").value(requisitosProjeto));
    }

    @Test
    void permiteTitulosIguaisComoProblemasDistintos() throws Exception {
        MvcResult primeiro = mockMvc.perform(post("/api/v1/problemas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonValido("Título repetido")))
                .andExpect(status().isCreated())
                .andReturn();
        MvcResult segundo = mockMvc.perform(post("/api/v1/problemas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonValido("Título repetido")))
                .andExpect(status().isCreated())
                .andReturn();

        long primeiroId = objectMapper.readTree(primeiro.getResponse().getContentAsString()).get("id").asLong();
        long segundoId = objectMapper.readTree(segundo.getResponse().getContentAsString()).get("id").asLong();
        org.assertj.core.api.Assertions.assertThat(primeiroId).isNotEqualTo(segundoId);
    }

    private Long criar(String titulo) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/v1/problemas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonValido(titulo)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    private ObjectNode payload(String titulo) {
        return objectMapper.createObjectNode()
                .put("titulo", titulo)
                .put("descricao", "Descrição")
                .put("requisitosFuncionais", "RF")
                .put("requisitosProjeto", "RP");
    }

    private String jsonValido(String titulo) throws Exception {
        return objectMapper.writeValueAsString(payload(titulo));
    }

    private String jsonComStatus(String titulo, String status) throws Exception {
        return objectMapper.writeValueAsString(payload(titulo).put("status", status));
    }

    private void aplicarInvalidade(ObjectNode payload, String campo, String tipo) {
        switch (tipo) {
            case "ausente" -> payload.remove(campo);
            case "nulo" -> payload.putNull(campo);
            case "em branco" -> payload.put(campo, " \t\n ");
            case "excesso" -> payload.put(campo, "x".repeat(LIMITES.get(campo) + 1));
            default -> throw new IllegalArgumentException("Tipo de invalidez desconhecido: " + tipo);
        }
    }
}
