package br.edu.ufcg.psoft.arena.estrategia.service;

import br.edu.ufcg.psoft.arena.estrategia.dto.AtualizarEstrategiaRequest;
import br.edu.ufcg.psoft.arena.estrategia.dto.CriarEstrategiaRequest;
import br.edu.ufcg.psoft.arena.estrategia.model.Estrategia;
import br.edu.ufcg.psoft.arena.estrategia.repository.EstrategiaRepository;
import br.edu.ufcg.psoft.arena.shared.exception.RecursoNaoEncontradoException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EstrategiaService {

    private final EstrategiaRepository repository;

    public EstrategiaService(EstrategiaRepository repository) {
        this.repository = repository;
    }

    public Estrategia criar(CriarEstrategiaRequest request) {
        Estrategia estrategia = new Estrategia(
                request.nome(),
                request.tipo(),
                request.descricao(),
                request.prompt());
        return repository.save(estrategia);
    }

    public List<Estrategia> listar() {
        return repository.findAll();
    }

    public Estrategia buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Estrategia " + id + " nao encontrada."));
    }

    public Estrategia atualizar(Long id, AtualizarEstrategiaRequest request) {
        Estrategia estrategia = buscarPorId(id);
        estrategia.atualizar(
                request.nome(),
                request.tipo(),
                request.descricao(),
                request.prompt());
        return repository.save(estrategia);
    }
}
