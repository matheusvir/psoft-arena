package br.edu.ufcg.psoft.arena.problema;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufcg.psoft.arena.problema.dto.ProblemaRequest;
import br.edu.ufcg.psoft.arena.problema.dto.ProblemaResponse;
import br.edu.ufcg.psoft.arena.shared.exception.RecursoNaoEncontradoException;

@Service
public class ProblemaService {

    private final ProblemaRepository repositorio;

    public ProblemaService(ProblemaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public ProblemaResponse criar(ProblemaRequest requisicao) {
        Problema problema = new Problema(
                requisicao.titulo(),
                requisicao.descricao(),
                requisicao.requisitosFuncionais(),
                requisicao.requisitosProjeto());
        return paraResposta(repositorio.save(problema));
    }

    @Transactional(readOnly = true)
    public List<ProblemaResponse> listar() {
        return repositorio.findAll().stream().map(this::paraResposta).toList();
    }

    @Transactional(readOnly = true)
    public ProblemaResponse buscarPorId(Long id) {
        return paraResposta(exigirExistente(id));
    }

    @Transactional
    public ProblemaResponse atualizar(Long id, ProblemaRequest requisicao) {
        Problema problema = exigirExistente(id);
        problema.atualizarDados(
                requisicao.titulo(),
                requisicao.descricao(),
                requisicao.requisitosFuncionais(),
                requisicao.requisitosProjeto());
        return paraResposta(repositorio.save(problema));
    }

    @Transactional
    public void desativar(Long id) {
        exigirExistente(id).desativar();
    }

    /**
     * Remove o problema no modelo atual, que ainda não possui relacionamento com atividades.
     * Esta operação não prova ausência de uso histórico; antes de atividades serem persistidas,
     * a integração de US6 deve ligar a consulta real {@link ConsultaUsoProblema} a esta regra.
     */
    @Transactional
    public void excluir(Long id) {
        exigirExistente(id);
        repositorio.deleteById(id);
    }

    private Problema exigirExistente(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Problema " + id + " não encontrado."));
    }

    private ProblemaResponse paraResposta(Problema problema) {
        return new ProblemaResponse(
                problema.getId(),
                problema.getTitulo(),
                problema.getDescricao(),
                problema.getRequisitosFuncionais(),
                problema.getRequisitosProjeto(),
                problema.getStatus());
    }
}
