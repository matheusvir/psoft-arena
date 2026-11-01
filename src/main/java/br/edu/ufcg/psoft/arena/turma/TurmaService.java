package br.edu.ufcg.psoft.arena.turma;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufcg.psoft.arena.professor.Professor;
import br.edu.ufcg.psoft.arena.professor.ProfessorRepository;
import br.edu.ufcg.psoft.arena.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TurmaService {

    private final TurmaRepository repository;
    private final ProfessorRepository professorRepository;

    @Transactional
    public TurmaResponse cadastrar(TurmaRequest request) {
        Professor professor = buscarProfessor(request.professorId());
        Turma turma = new Turma(request.nome(), request.codigo(), request.periodoLetivo(), professor);
        return paraResponse(repository.saveAndFlush(turma));
    }

    public List<TurmaResponse> listar() {
        return repository.findAll().stream().map(this::paraResponse).toList();
    }

    public TurmaResponse consultar(Long id) {
        return paraResponse(buscarTurma(id));
    }

    @Transactional
    public TurmaResponse atualizar(Long id, TurmaRequest request) {
        Turma turma = buscarTurma(id);
        Professor professor = buscarProfessor(request.professorId());
        turma.atualizar(request.nome(), request.codigo(), request.periodoLetivo(), professor);
        return paraResponse(repository.saveAndFlush(turma));
    }

    @Transactional
    public void encerrar(Long id) {
        buscarTurma(id).encerrar();
    }

    private Turma buscarTurma(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Turma " + id + " nao encontrada."));
    }

    private Professor buscarProfessor(Long id) {
        return professorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Professor " + id + " nao encontrado."));
    }

    private TurmaResponse paraResponse(Turma turma) {
        return new TurmaResponse(turma.getId(), turma.getNome(), turma.getCodigo(), turma.getPeriodoLetivo(),
                turma.getProfessor().getId(), turma.getStatus());
    }
}
