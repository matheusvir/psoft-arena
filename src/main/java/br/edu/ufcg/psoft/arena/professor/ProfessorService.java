package br.edu.ufcg.psoft.arena.professor;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufcg.psoft.arena.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfessorService {

    private final ProfessorRepository repository;

    @Transactional
    public ProfessorResponse cadastrar(ProfessorRequest request) {
        Professor professor = new Professor(request.nome(), request.email());
        return paraResponse(repository.saveAndFlush(professor));
    }

    public List<ProfessorResponse> listar() {
        return repository.findAll().stream().map(this::paraResponse).toList();
    }

    public ProfessorResponse consultar(Long id) {
        Professor professor = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Professor " + id + " nao encontrado."));
        return paraResponse(professor);
    }

    private ProfessorResponse paraResponse(Professor professor) {
        return new ProfessorResponse(professor.getId(), professor.getNome(), professor.getEmail());
    }
}
