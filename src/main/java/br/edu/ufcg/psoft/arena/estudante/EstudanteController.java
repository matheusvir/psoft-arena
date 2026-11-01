package br.edu.ufcg.psoft.arena.estudante;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/estudantes")
@Tag(name = "Estudantes")
public class EstudanteController {

    private final EstudanteService estudanteService;

    public EstudanteController(EstudanteService estudanteService) {
        this.estudanteService = estudanteService;
    }

    @PostMapping
    @Operation(summary = "Cadastrar estudante", description = "Aplica trim nos campos e minusculas no email antes "
            + "de validar. Matricula e email sao unicos.")
    @ApiResponse(responseCode = "201", description = "Estudante criado; Location aponta para a consulta",
            useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Campos invalidos; erros por campo na propriedade erros",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Matricula ou email ja cadastrado",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<EstudanteResponseDTO> criarEstudante(@Valid @RequestBody EstudanteRequestDTO dados) {
        var estudante = estudanteService.criarEstudante(dados);
        var localizacao = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(estudante.getId()).toUri();
        return ResponseEntity.created(localizacao).body(estudante);
    }

    @GetMapping
    @Operation(summary = "Listar estudantes", description = "Sem paginacao e sem ordenacao garantida")
    @ApiResponse(responseCode = "200", description = "Lista de estudantes", useReturnTypeSchema = true)
    public List<EstudanteResponseDTO> listarEstudantes() {
        return estudanteService.listarEstudantes();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar estudante por ID")
    @ApiResponse(responseCode = "200", description = "Estudante encontrado", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", description = "Estudante inexistente",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public EstudanteResponseDTO consultarEstudantePorId(@PathVariable Long id) {
        return estudanteService.consultarEstudantePorId(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar estudante", description = "Substitui nome, matricula e email. Aplica trim e email "
            + "em minusculas antes de validar; permite manter os proprios identificadores.")
    @ApiResponse(responseCode = "200", description = "Estudante atualizado", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Campos invalidos; erros por campo na propriedade erros",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Estudante inexistente",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Matricula ou email pertence a outro estudante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public EstudanteResponseDTO editarEstudante(@PathVariable Long id, @Valid @RequestBody EstudanteRequestDTO dados) {
        return estudanteService.editarEstudante(id, dados);
    }
}
