package br.edu.ufcg.psoft.arena.estrategia.controller;

import br.edu.ufcg.psoft.arena.estrategia.dto.AtualizarEstrategiaRequest;
import br.edu.ufcg.psoft.arena.estrategia.dto.CriarEstrategiaRequest;
import br.edu.ufcg.psoft.arena.estrategia.dto.EstrategiaResponse;
import br.edu.ufcg.psoft.arena.estrategia.service.EstrategiaService;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/estrategias")
public class EstrategiaController {

    private final EstrategiaService service;

    public EstrategiaController(EstrategiaService service) {
        this.service = service;
    }

    @PostMapping
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Estrategia criada",
                headers = @Header(name = "Location", description = "URI da estrategia criada",
                        schema = @Schema(type = "string", format = "uri")),
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = EstrategiaResponse.class))),
        @ApiResponse(responseCode = "400", description = "Requisicao invalida",
                content = @Content(mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<EstrategiaResponse> criar(@Valid @RequestBody CriarEstrategiaRequest request) {
        EstrategiaResponse response = EstrategiaResponse.de(service.criar(request));
        URI localizacao = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(localizacao).body(response);
    }

    @GetMapping
    @ApiResponse(responseCode = "200", description = "Lista de estrategias")
    public List<EstrategiaResponse> listar() {
        return service.listar().stream()
                .map(EstrategiaResponse::de)
                .toList();
    }

    @GetMapping("/{id}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estrategia encontrada",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = EstrategiaResponse.class))),
        @ApiResponse(responseCode = "400", description = "Identificador invalido",
                content = @Content(mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Estrategia nao encontrada",
                content = @Content(mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class)))
    })
    public EstrategiaResponse buscarPorId(@PathVariable Long id) {
        return EstrategiaResponse.de(service.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estrategia atualizada",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = EstrategiaResponse.class))),
        @ApiResponse(responseCode = "400", description = "Requisicao invalida",
                content = @Content(mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Estrategia nao encontrada",
                content = @Content(mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class)))
    })
    public EstrategiaResponse atualizar(@PathVariable Long id,
            @Valid @RequestBody AtualizarEstrategiaRequest request) {
        return EstrategiaResponse.de(service.atualizar(id, request));
    }
}
