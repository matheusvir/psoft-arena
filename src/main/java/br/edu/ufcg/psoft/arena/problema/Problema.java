package br.edu.ufcg.psoft.arena.problema;

import br.edu.ufcg.psoft.arena.shared.entity.BaseEntity;
import br.edu.ufcg.psoft.arena.shared.exception.RegraDeNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;

/** Entidade que representa um problema cadastrável na plataforma. */
@Getter
@Entity
@Table(name = "problema")
public class Problema extends BaseEntity {

    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    @Column(name = "descricao", nullable = false, length = 2000)
    private String descricao;

    @Column(name = "requisitos_funcionais", nullable = false, length = 4000)
    private String requisitosFuncionais;

    @Column(name = "requisitos_projeto", nullable = false, length = 4000)
    private String requisitosProjeto;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusProblema status;

    protected Problema() {
        // Construtor exigido pelo JPA.
    }

    public Problema(
            String titulo,
            String descricao,
            String requisitosFuncionais,
            String requisitosProjeto) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.requisitosFuncionais = requisitosFuncionais;
        this.requisitosProjeto = requisitosProjeto;
        this.status = StatusProblema.ATIVO;
    }

    /** Atualiza os dados cadastrais sem permitir alteração do status. */
    public void atualizarDados(
            String titulo,
            String descricao,
            String requisitosFuncionais,
            String requisitosProjeto) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.requisitosFuncionais = requisitosFuncionais;
        this.requisitosProjeto = requisitosProjeto;
    }

    /** Transiciona o problema ativo para inativo; repetir o comando não altera o estado. */
    public void desativar() {
        if (status == StatusProblema.ATIVO) {
            status = StatusProblema.INATIVO;
        }
    }

    /** Exige estado ativo antes que um consumidor associe o problema a uma nova atividade. */
    public void exigirAtivoParaNovaAtividade() {
        if (status != StatusProblema.ATIVO) {
            throw new RegraDeNegocioException("Problema inativo não pode ser associado a nova atividade.");
        }
    }

    /** Bloqueia a remoção quando uma consulta externa comprova uso histórico. */
    public void validarPodeRemover(boolean jaFoiUtilizado) {
        if (jaFoiUtilizado) {
            throw new RegraDeNegocioException("Problema já foi utilizado em uma atividade e não pode ser removido.");
        }
    }
}
