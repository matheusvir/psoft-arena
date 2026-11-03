package br.edu.ufcg.psoft.arena.turma;

import java.net.URI;
import java.util.List;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/turmas/{id}/estudantes")
@RequiredArgsConstructor
@Tag(name = "TurmaEstudantes")
public class TurmaEstudanteController {

    private final TurmaEstudanteService service;

    @PostMapping
    @Operation(summary = "Associar estudante a turma ativa")
    @ApiResponse(responseCode = "201", description = "Vinculo criado; Location aponta para o vinculo",
            headers = @Header(name = "Location", description = "URI do vinculo criado",
                    schema = @Schema(type = "string", format = "uri")),
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = TurmaEstudanteResponse.class)))
    @ApiResponse(responseCode = "400", description = "Corpo invalido",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Turma ou estudante inexistente",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Turma encerrada ou vinculo duplicado",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<TurmaEstudanteResponse> adicionar(
            @PathVariable Long id, @Valid @RequestBody TurmaEstudanteRequest request) {
        TurmaEstudanteResponse response = service.adicionar(id, request.estudanteId());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{estudanteId}").buildAndExpand(response.estudanteId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar estudantes associados a turma")
    @ApiResponse(responseCode = "200", description = "Lista de vinculos da turma", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", description = "Turma inexistente",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public List<TurmaEstudanteResponse> listar(@PathVariable Long id) {
        return service.listar(id);
    }

    @GetMapping("/{estudanteId}")
    @Operation(summary = "Consultar vinculo de estudante na turma")
    @ApiResponse(responseCode = "200", description = "Vinculo encontrado", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", description = "Turma ou vinculo inexistente",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public TurmaEstudanteResponse consultar(@PathVariable Long id, @PathVariable Long estudanteId) {
        return service.consultar(id, estudanteId);
    }

    @DeleteMapping("/{estudanteId}")
    @Operation(summary = "Remover vinculo de estudante em turma ativa")
    @ApiResponse(responseCode = "204", description = "Vinculo removido", content = @Content)
    @ApiResponse(responseCode = "404", description = "Turma inexistente ou vinculo inexistente",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Turma encerrada",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> remover(@PathVariable Long id, @PathVariable Long estudanteId) {
        service.remover(id, estudanteId);
        return ResponseEntity.noContent().build();
    }
}
