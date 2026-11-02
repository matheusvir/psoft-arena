package br.edu.ufcg.psoft.arena.turma;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufcg.psoft.arena.estudante.Estudante;
import br.edu.ufcg.psoft.arena.estudante.EstudanteRepository;
import br.edu.ufcg.psoft.arena.shared.exception.RecursoNaoEncontradoException;
import br.edu.ufcg.psoft.arena.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TurmaEstudanteService {

    private final TurmaRepository turmaRepository;
    private final EstudanteRepository estudanteRepository;
    private final TurmaEstudanteRepository repository;

    @Transactional
    public TurmaEstudanteResponse adicionar(Long turmaId, Long estudanteId) {
        Turma turma = buscarTurma(turmaId);
        Estudante estudante = buscarEstudante(estudanteId);
        if (repository.existsByTurmaIdAndEstudanteId(turmaId, estudanteId)) {
            throw new RegraDeNegocioException("Estudante ja associado a turma.");
        }
        TurmaEstudante vinculo = turma.adicionarEstudante(estudante);
        return paraResponse(repository.saveAndFlush(vinculo));
    }

    public List<TurmaEstudanteResponse> listar(Long turmaId) {
        buscarTurma(turmaId);
        return repository.findAllByTurmaId(turmaId).stream().map(this::paraResponse).toList();
    }

    @Transactional
    public void remover(Long turmaId, Long estudanteId) {
        Turma turma = buscarTurma(turmaId);
        TurmaEstudante vinculo = repository.findByTurmaIdAndEstudanteId(turmaId, estudanteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Vinculo entre turma " + turmaId + " e estudante " + estudanteId + " nao encontrado."));
        turma.removerEstudante(vinculo);
        repository.delete(vinculo);
    }

    private Turma buscarTurma(Long id) {
        return turmaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Turma " + id + " nao encontrada."));
    }

    private Estudante buscarEstudante(Long id) {
        return estudanteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Estudante " + id + " nao encontrado."));
    }

    private TurmaEstudanteResponse paraResponse(TurmaEstudante vinculo) {
        Estudante estudante = vinculo.getEstudante();
        return new TurmaEstudanteResponse(
                estudante.getId(), estudante.getNome(), estudante.getMatricula(), vinculo.getAssociadoEm());
    }
}
