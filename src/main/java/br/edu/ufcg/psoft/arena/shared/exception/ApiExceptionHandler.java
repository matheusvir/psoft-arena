package br.edu.ufcg.psoft.arena.shared.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduz excecoes conhecidas para {@link ProblemDetail} (RFC 9457).
 *
 * <p>400 para payload invalido, 404 para recurso inexistente e 409 para regra de negocio,
 * duplicata ou violacao de FK.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail tratarCorpoInvalido(HttpMessageNotReadableException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Corpo da requisicao ausente ou com formato ou valores invalidos.");
        problema.setTitle("Requisicao invalida");
        return problema;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail tratarParametroInvalido(MethodArgumentTypeMismatchException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Parametro '" + excecao.getName() + "' com formato invalido.");
        problema.setTitle("Requisicao invalida");
        return problema;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail tratarValidacao(MethodArgumentNotValidException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Um ou mais campos da requisicao sao invalidos.");
        problema.setTitle("Requisicao invalida");

        Map<String, String> erros = new LinkedHashMap<>();
        for (FieldError erro : excecao.getBindingResult().getFieldErrors()) {
            erros.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }
        problema.setProperty("erros", erros);
        return problema;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail tratarCorpoInvalido(HttpMessageNotReadableException excecao) {
        return problemaRequisicaoInvalida("O corpo da requisicao esta ausente ou malformado.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail tratarTipoDeParametroInvalido(MethodArgumentTypeMismatchException excecao) {
        return problemaRequisicaoInvalida("Um parametro da requisicao possui tipo invalido.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail tratarIntegridade(DataIntegrityViolationException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "Operacao viola uma restricao de integridade dos dados.");
        problema.setTitle("Conflito de dados");
        return problema;
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ProblemDetail tratarRegraDeNegocio(RegraDeNegocioException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, excecao.getMessage());
        problema.setTitle("Regra de negocio violada");
        return problema;
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail tratarNaoEncontrado(RecursoNaoEncontradoException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, excecao.getMessage());
        problema.setTitle("Recurso nao encontrado");
        return problema;
    }

    private ProblemDetail problemaRequisicaoInvalida(String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalhe);
        problema.setTitle("Requisicao invalida");
        return problema;
    }
}
