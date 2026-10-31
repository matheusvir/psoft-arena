package br.edu.uepb.psoft.arena.shared.exception;

/**
 * Violacao de regra de negocio mapeada para HTTP 409.
 */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }

    public RegraDeNegocioException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
