package br.edu.ufcg.psoft.arena.problema;

/**
 * Port para consultar uso histórico de um problema.
 *
 * <p>O provider deve retornar {@code true} se qualquer atividade já usou o problema,
 * inclusive atividade concluída. {@code false} só pode significar que a fonte autoritativa
 * comprova que nunca houve uso; ausência de uma relação corrente não basta se atividades ou
 * vínculos puderem ser removidos. A implementação e o wiring pertencem à integração com US6.
 * Esta interface não é um bean de produção nesta etapa.
 */
@FunctionalInterface
public interface ConsultaUsoProblema {

    /**
     * Informa se o problema já foi usado por alguma atividade em qualquer momento.
     *
     * @param problemaId identificador do problema consultado
     * @return {@code true} para qualquer uso histórico, inclusive atividade concluída
     */
    boolean jaFoiUtilizado(Long problemaId);
}
