package br.edu.ufcg.psoft.arena.estrategia.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import br.edu.ufcg.psoft.arena.estrategia.dto.AtualizarEstrategiaRequest;
import br.edu.ufcg.psoft.arena.estrategia.dto.CriarEstrategiaRequest;
import br.edu.ufcg.psoft.arena.estrategia.model.Estrategia;
import br.edu.ufcg.psoft.arena.estrategia.model.TipoEstrategia;
import br.edu.ufcg.psoft.arena.estrategia.repository.EstrategiaRepository;
import br.edu.ufcg.psoft.arena.shared.exception.RecursoNaoEncontradoException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EstrategiaServiceTest {

    @Mock
    private EstrategiaRepository repository;

    @InjectMocks
    private EstrategiaService service;

    @Test
    void criaEstrategiaComOsDadosDoRequest() {
        CriarEstrategiaRequest request = requestCriacao();
        when(repository.save(any(Estrategia.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Estrategia resultado = service.criar(request);

        assertThat(resultado.getNome()).isEqualTo("Code Smells");
        assertThat(resultado.getTipo()).isEqualTo(TipoEstrategia.CODE_SMELLS);
        assertThat(resultado.getDescricao()).isEqualTo("Detecta mas praticas.");
        assertThat(resultado.getPrompt()).isEqualTo("Analise code smells.");
        verify(repository).save(any(Estrategia.class));
    }

    @Test
    void listaTodasAsEstrategias() {
        Estrategia primeira = estrategia("Primeira");
        Estrategia segunda = estrategia("Segunda");
        when(repository.findAll()).thenReturn(List.of(primeira, segunda));

        assertThat(service.listar()).containsExactly(primeira, segunda);
    }

    @Test
    void buscaEstrategiaExistente() {
        Estrategia estrategia = estrategia("Existente");
        when(repository.findById(7L)).thenReturn(Optional.of(estrategia));

        assertThat(service.buscarPorId(7L)).isSameAs(estrategia);
    }

    @Test
    void buscaEstrategiaInexistenteLancaExcecaoPadronizada() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Estrategia 99 nao encontrada.");
    }

    @Test
    void atualizaTodosOsDadosInclusiveOPrompt() {
        Estrategia estrategia = estrategia("Antiga");
        when(repository.findById(3L)).thenReturn(Optional.of(estrategia));
        when(repository.save(any(Estrategia.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Estrategia resultado = service.atualizar(3L, new AtualizarEstrategiaRequest(
                "Nova",
                TipoEstrategia.PADROES_DE_PROJETO,
                "Nova descricao",
                "Novo prompt"));

        assertThat(resultado.getNome()).isEqualTo("Nova");
        assertThat(resultado.getTipo()).isEqualTo(TipoEstrategia.PADROES_DE_PROJETO);
        assertThat(resultado.getDescricao()).isEqualTo("Nova descricao");
        assertThat(resultado.getPrompt()).isEqualTo("Novo prompt");
        verify(repository).save(estrategia);
    }

    @Test
    void atualizarEstrategiaInexistenteLancaExcecaoSemSalvar() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(99L, new AtualizarEstrategiaRequest(
                "Nova", TipoEstrategia.CODE_SMELLS, "Descricao", "Prompt")))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verifyNoMoreInteractions(repository);
    }

    private CriarEstrategiaRequest requestCriacao() {
        return new CriarEstrategiaRequest(
                "Code Smells",
                TipoEstrategia.CODE_SMELLS,
                "Detecta mas praticas.",
                "Analise code smells.");
    }

    private Estrategia estrategia(String nome) {
        return new Estrategia(nome, TipoEstrategia.CODE_SMELLS, "Descricao", "Prompt");
    }
}
