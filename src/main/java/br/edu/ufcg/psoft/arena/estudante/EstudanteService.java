package br.edu.ufcg.psoft.arena.estudante;

import java.util.List;

import br.edu.ufcg.psoft.arena.shared.exception.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EstudanteService {

    private final EstudanteRepository estudanteRepository;

    public EstudanteService(EstudanteRepository estudanteRepository) {
        this.estudanteRepository = estudanteRepository;
    }

    @Transactional
    public EstudanteResponseDTO criar(EstudanteRequestDTO dados) {
        var estudante = new Estudante(dados.getNome(), dados.getMatricula(), dados.getEmail());
        var estudantePersistido = estudanteRepository.saveAndFlush(estudante);
        return representar(estudantePersistido);
    }

    public List<EstudanteResponseDTO> listar() {
        var estudantes = estudanteRepository.findAll();
        return estudantes.stream().map(this::representar).toList();
    }

    public EstudanteResponseDTO consultar(Long id) {
        var estudante = buscarPorId(id);
        return representar(estudante);
    }

    @Transactional
    public EstudanteResponseDTO editar(Long id, EstudanteRequestDTO dados) {
        var estudante = buscarPorId(id);
        estudante.atualizarDados(dados.getNome(), dados.getMatricula(), dados.getEmail());
        var estudantePersistido = estudanteRepository.saveAndFlush(estudante);
        return representar(estudantePersistido);
    }

    private Estudante buscarPorId(Long id) {
        return estudanteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Estudante " + id + " nao encontrado."));
    }

    private EstudanteResponseDTO representar(Estudante estudante) {
        return new EstudanteResponseDTO(estudante.getId(), estudante.getNome(), estudante.getMatricula(),
                estudante.getEmail());
    }
}
