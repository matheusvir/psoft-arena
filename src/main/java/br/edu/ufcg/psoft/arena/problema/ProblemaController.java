package br.edu.ufcg.psoft.arena.problema;

import java.net.URI;
import java.util.List;

import br.edu.ufcg.psoft.arena.problema.dto.ProblemaRequest;
import br.edu.ufcg.psoft.arena.problema.dto.ProblemaResponse;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/problemas")
public class ProblemaController {

    private final ProblemaService servico;

    public ProblemaController(ProblemaService servico) {
        this.servico = servico;
    }

    @PostMapping
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "201",
                description = "Problema criado.",
                headers = @Header(
                        name = "Location",
                        description = "URI do problema criado.",
                        schema = @Schema(type = "string", format = "uri")),
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ProblemaResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Falha de validação dos campos; o `ProblemDetail` contém o mapa `erros` por campo. Sintaxe ou corpo inválido pode seguir o tratamento padrão.",
                content = @Content(
                        mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ProblemaResponse> criar(@Valid @RequestBody ProblemaRequest requisicao) {
        ProblemaResponse resposta = servico.criar(requisicao);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resposta.id())
                .toUri();
        return ResponseEntity.created(location).body(resposta);
    }

    @GetMapping
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Lista de problemas.",
                content = @Content(
                        mediaType = "application/json",
                        array = @ArraySchema(schema = @Schema(implementation = ProblemaResponse.class))))
    })
    public List<ProblemaResponse> listar() {
        return servico.listar();
    }

    @GetMapping("/{id}")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Problema encontrado.",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ProblemaResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Problema não encontrado.",
                content = @Content(
                        mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ProblemaResponse buscarPorId(@PathVariable Long id) {
        return servico.buscarPorId(id);
    }

    @PutMapping("/{id}")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Problema atualizado; o status existente é preservado.",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ProblemaResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Falha de validação dos campos; o `ProblemDetail` contém o mapa `erros` por campo. Sintaxe ou corpo inválido pode seguir o tratamento padrão.",
                content = @Content(
                        mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Problema não encontrado.",
                content = @Content(
                        mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ProblemaResponse atualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProblemaRequest requisicao) {
        return servico.atualizar(id, requisicao);
    }

    @PatchMapping("/{id}/desativar")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Problema desativado.", content = @Content),
        @ApiResponse(
                responseCode = "404",
                description = "Problema não encontrado.",
                content = @Content(
                        mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        servico.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Problema removido no modelo atual.", content = @Content),
        @ApiResponse(
                responseCode = "404",
                description = "Problema não encontrado.",
                content = @Content(
                        mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Conflito de negócio previsto pelo contrato HTTP; a consulta real de uso histórico de US6 ainda não está integrada.",
                content = @Content(
                        mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        servico.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
