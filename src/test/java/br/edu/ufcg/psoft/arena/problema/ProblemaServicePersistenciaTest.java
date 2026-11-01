package br.edu.ufcg.psoft.arena.problema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufcg.psoft.arena.problema.dto.ProblemaRequest;
import br.edu.ufcg.psoft.arena.problema.dto.ProblemaResponse;
import br.edu.ufcg.psoft.arena.shared.exception.RecursoNaoEncontradoException;

@SpringBootTest
@Transactional
class ProblemaServicePersistenciaTest {

    @Autowired
    private ProblemaService servico;

    @Test
    void criarPersistirEConsultarTodosOsCampos() {
        String requisitosFuncionais = "  requisito funcional\nsegunda linha\r\n";
        String requisitosProjeto = " requisito de projeto\tcom tab ";

        ProblemaResponse criado = servico.criar(
                request("Título", "Descrição", requisitosFuncionais, requisitosProjeto));
        ProblemaResponse consultado = servico.buscarPorId(criado.id());

        assertThat(consultado.titulo()).isEqualTo("Título");
        assertThat(consultado.descricao()).isEqualTo("Descrição");
        assertThat(consultado.requisitosFuncionais()).isEqualTo(requisitosFuncionais);
        assertThat(consultado.requisitosProjeto()).isEqualTo(requisitosProjeto);
        assertThat(consultado.status()).isEqualTo(StatusProblema.ATIVO);
    }

    @Test
    void atualizarProblemaInativoPreservaStatus() {
        ProblemaResponse criado = servico.criar(request("Título antigo", "Descrição antiga", "RF antigo", "RP antigo"));
        servico.desativar(criado.id());

        ProblemaResponse atualizado = servico.atualizar(
                criado.id(), request("Título novo", "Descrição nova", "RF novo", "RP novo"));

        assertThat(atualizado.titulo()).isEqualTo("Título novo");
        assertThat(atualizado.descricao()).isEqualTo("Descrição nova");
        assertThat(atualizado.requisitosFuncionais()).isEqualTo("RF novo");
        assertThat(atualizado.requisitosProjeto()).isEqualTo("RP novo");
        assertThat(atualizado.status()).isEqualTo(StatusProblema.INATIVO);
    }

    @Test
    void listarIncluiProblemasAtivosEInativosEPermiteTitulosIguais() {
        ProblemaResponse primeiro = servico.criar(request("Mesmo título", "Descrição 1", "RF 1", "RP 1"));
        ProblemaResponse segundo = servico.criar(request("Mesmo título", "Descrição 2", "RF 2", "RP 2"));
        servico.desativar(segundo.id());

        assertThat(servico.listar())
                .extracting(ProblemaResponse::status)
                .contains(StatusProblema.ATIVO, StatusProblema.INATIVO);
        assertThat(servico.buscarPorId(primeiro.id()).titulo())
                .isEqualTo(servico.buscarPorId(segundo.id()).titulo());
        assertThat(primeiro.id()).isNotEqualTo(segundo.id());
    }

    @Test
    void excluirProblemaNoModeloAtualRemoveRegistro() {
        ProblemaResponse criado = servico.criar(request("Título", "Descrição", "RF", "RP"));

        servico.excluir(criado.id());

        assertThatThrownBy(() -> servico.buscarPorId(criado.id()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void operacoesComIdInexistenteRetornamRecursoNaoEncontrado() {
        assertThatThrownBy(() -> servico.buscarPorId(-1L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> servico.atualizar(-1L, request("Título", "Descrição", "RF", "RP")))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> servico.desativar(-1L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> servico.excluir(-1L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private ProblemaRequest request(String titulo, String descricao, String requisitosFuncionais, String requisitosProjeto) {
        return new ProblemaRequest(titulo, descricao, requisitosFuncionais, requisitosProjeto);
    }
}
