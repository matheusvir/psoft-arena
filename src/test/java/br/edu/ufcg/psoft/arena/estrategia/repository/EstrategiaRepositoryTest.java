package br.edu.ufcg.psoft.arena.estrategia.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.ufcg.psoft.arena.estrategia.model.Estrategia;
import br.edu.ufcg.psoft.arena.estrategia.model.TipoEstrategia;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class EstrategiaRepositoryTest {

    @Autowired
    private EstrategiaRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persisteERecuperaOsQuatroCampos() {
        Estrategia salva = repository.saveAndFlush(new Estrategia(
                "Code Smells",
                TipoEstrategia.CODE_SMELLS,
                "Detecta mas praticas.",
                "Analise code smells."));

        entityManager.clear();
        Estrategia recuperada = repository.findById(salva.getId()).orElseThrow();

        assertThat(recuperada.getNome()).isEqualTo("Code Smells");
        assertThat(recuperada.getTipo()).isEqualTo(TipoEstrategia.CODE_SMELLS);
        assertThat(recuperada.getDescricao()).isEqualTo("Detecta mas praticas.");
        assertThat(recuperada.getPrompt()).isEqualTo("Analise code smells.");
    }
}
