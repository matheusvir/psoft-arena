package br.edu.ufcg.psoft.arena.estudante;

import java.util.List;

import br.edu.ufcg.psoft.arena.shared.exception.RecursoNaoEncontradoException;
import br.edu.ufcg.psoft.arena.shared.exception.RegraDeNegocioException;
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
    public EstudanteResponseDTO criarEstudante(EstudanteRequestDTO dados) {
        validarIdentificadoresUnicos(null, dados.getMatricula(), dados.getEmail());
        Estudante estudante = new Estudante(dados.getNome(), dados.getMatricula(), dados.getEmail());
        Estudante estudantePersistido = estudanteRepository.saveAndFlush(estudante);
        return converterParaResponseDTO(estudantePersistido);
    }

    public List<EstudanteResponseDTO> listarEstudantes() {
        List<Estudante> estudantes = estudanteRepository.findAll();
        return estudantes.stream().map(this::converterParaResponseDTO).toList();
    }

    public EstudanteResponseDTO consultarEstudantePorId(Long id) {
        Estudante estudante = buscarPorId(id);
        return converterParaResponseDTO(estudante);
    }

    @Transactional
    public EstudanteResponseDTO editarEstudante(Long id, EstudanteRequestDTO dados) {
        Estudante estudante = buscarPorId(id);
        validarIdentificadoresUnicos(id, dados.getMatricula(), dados.getEmail());
        estudante.atualizarDados(dados.getNome(), dados.getMatricula(), dados.getEmail());
        Estudante estudantePersistido = estudanteRepository.saveAndFlush(estudante);
        return converterParaResponseDTO(estudantePersistido);
    }

    private Estudante buscarPorId(Long id) {
        return estudanteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Estudante " + id + " nao encontrado."));
    }

    private void validarIdentificadoresUnicos(Long idIgnorado, String matricula, String email) {
        estudanteRepository.findByMatricula(matricula)
                .filter(outro -> !outro.getId().equals(idIgnorado))
                .ifPresent(outro -> {
                    throw new RegraDeNegocioException("Matricula ja cadastrada.");
                });
        estudanteRepository.findByEmail(email)
                .filter(outro -> !outro.getId().equals(idIgnorado))
                .ifPresent(outro -> {
                    throw new RegraDeNegocioException("Email ja cadastrado.");
                });
    }

    private EstudanteResponseDTO converterParaResponseDTO(Estudante estudante) {
        return new EstudanteResponseDTO(estudante.getId(), estudante.getNome(), estudante.getMatricula(),
                estudante.getEmail());
    }
}
