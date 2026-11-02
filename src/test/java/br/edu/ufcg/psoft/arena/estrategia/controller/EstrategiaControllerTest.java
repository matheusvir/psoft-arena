package br.edu.ufcg.psoft.arena.estrategia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.ufcg.psoft.arena.estrategia.model.Estrategia;
import br.edu.ufcg.psoft.arena.estrategia.model.TipoEstrategia;
import br.edu.ufcg.psoft.arena.estrategia.repository.EstrategiaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class EstrategiaControllerTest {

    private static final String CRIACAO_VALIDA = """
            {
              "nome": "Analise de Code Smells",
              "tipo": "CODE_SMELLS",
              "descricao": "Detecta mas praticas no codigo.",
              "prompt": "Analise o codigo procurando code smells."
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EstrategiaRepository repository;

    @BeforeEach
    void limparBanco() {
        repository.deleteAll();
    }

    @Test
    void criaEstrategiaCom201LocationETodosOsCampos() throws Exception {
        enviarPost(CRIACAO_VALIDA)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(
                        ".*/api/v1/estrategias/[0-9]+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Analise de Code Smells"))
                .andExpect(jsonPath("$.tipo").value("CODE_SMELLS"))
                .andExpect(jsonPath("$.descricao").value("Detecta mas praticas no codigo."))
                .andExpect(jsonPath("$.prompt").value("Analise o codigo procurando code smells."));
    }

    @Test
    void listaEConsultaEstrategias() throws Exception {
        Estrategia primeira = repository.save(new Estrategia(
                "Primeira", TipoEstrategia.CODE_SMELLS, "Descricao 1", "Prompt 1"));

        repository.save(new Estrategia(
                "Segunda", TipoEstrategia.PADROES_DE_PROJETO, "Descricao 2", "Prompt 2"));

        mockMvc.perform(get("/api/v1/estrategias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nome").value(
                        org.hamcrest.Matchers.containsInAnyOrder("Primeira", "Segunda")));

        mockMvc.perform(get("/api/v1/estrategias/{id}", primeira.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Primeira"))
                .andExpect(jsonPath("$.prompt").value("Prompt 1"));
    }

    @Test
    void atualizaTodosOsCamposInclusiveOPrompt() throws Exception {
        Estrategia estrategia = repository.save(new Estrategia(
                "Antiga", TipoEstrategia.CODE_SMELLS, "Descricao antiga", "Prompt antigo"));

        enviarPut(estrategia.getId(), """
                {
                  "nome": "Nova",
                  "tipo": "ACOPLAMENTO_COESAO_PRINCIPIOS",
                  "descricao": "Descricao nova",
                  "prompt": "Prompt novo"
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nova"))
                .andExpect(jsonPath("$.tipo").value("ACOPLAMENTO_COESAO_PRINCIPIOS"))
                .andExpect(jsonPath("$.descricao").value("Descricao nova"))
                .andExpect(jsonPath("$.prompt").value("Prompt novo"));
    }

    @Test
    void retorna404ParaEstrategiaInexistente() throws Exception {
        mockMvc.perform(get("/api/v1/estrategias/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Estrategia 999999 nao encontrada."));

        enviarPut(999999L, CRIACAO_VALIDA)
                .andExpect(status().isNotFound());
    }

    @Test
    void rejeitaDadosInvalidos() throws Exception {
        enviarPost("""
                {
                  "nome": "   ",
                  "tipo": null,
                  "descricao": "",
                  "prompt": "   "
                }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").exists())
                .andExpect(jsonPath("$.erros.tipo").exists())
                .andExpect(jsonPath("$.erros.descricao").exists())
                .andExpect(jsonPath("$.erros.prompt").exists());

        enviarPost(CRIACAO_VALIDA.replace("CODE_SMELLS", "TIPO_INVALIDO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aceitaTiposValidosERepeteUmTipo() throws Exception {
        criarTipo("CODE_SMELLS", "Code Smells");
        criarTipo("CODE_SMELLS", "Code Smells 2");
        criarTipo("ACOPLAMENTO_COESAO_PRINCIPIOS", "Principios");
        criarTipo("PADROES_DE_PROJETO", "Padroes");
        assertThat(repository.findAll().stream()
                .filter(estrategia -> estrategia.getTipo() == TipoEstrategia.CODE_SMELLS)
                .map(Estrategia::getPrompt).toList())
                .containsExactlyInAnyOrder("Prompt Code Smells", "Prompt Code Smells 2");
    }

    @Test
    void rejeitaValoresAcimaDosLimites() throws Exception {
        String json = "{\"nome\":\"" + "n".repeat(121)
                + "\",\"tipo\":\"CODE_SMELLS\",\"descricao\":\"" + "d".repeat(2001)
                + "\",\"prompt\":\"" + "p".repeat(8001) + "\"}";

        enviarPost(json).andExpect(status().isBadRequest());
    }

    private void criarTipo(String tipo, String nome) throws Exception {
        String json = "{\"nome\":\"" + nome + "\",\"tipo\":\"" + tipo
                + "\",\"descricao\":\"Descricao\",\"prompt\":\"Prompt " + nome + "\"}";

        enviarPost(json)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value(tipo))
                .andExpect(jsonPath("$.prompt").value("Prompt " + nome));
    }

    private ResultActions enviarPost(String json) throws Exception {
        return mockMvc.perform(post("/api/v1/estrategias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions enviarPut(Long id, String json) throws Exception {
        return mockMvc.perform(put("/api/v1/estrategias/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }
}
