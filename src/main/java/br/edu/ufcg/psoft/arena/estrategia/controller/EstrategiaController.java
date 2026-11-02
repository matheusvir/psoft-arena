package br.edu.ufcg.psoft.arena.estrategia.controller;

import br.edu.ufcg.psoft.arena.estrategia.dto.AtualizarEstrategiaRequest;
import br.edu.ufcg.psoft.arena.estrategia.dto.CriarEstrategiaRequest;
import br.edu.ufcg.psoft.arena.estrategia.dto.EstrategiaResponse;
import br.edu.ufcg.psoft.arena.estrategia.service.EstrategiaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
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
@RequestMapping("/api/v1/estrategias")
public class EstrategiaController {

    private final EstrategiaService service;

    public EstrategiaController(EstrategiaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<EstrategiaResponse> criar(@Valid @RequestBody CriarEstrategiaRequest request) {
        EstrategiaResponse response = EstrategiaResponse.de(service.criar(request));
        URI localizacao = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(localizacao).body(response);
    }

    @GetMapping
    public List<EstrategiaResponse> listar() {
        return service.listar().stream()
                .map(EstrategiaResponse::de)
                .toList();
    }

    @GetMapping("/{id}")
    public EstrategiaResponse buscarPorId(@PathVariable Long id) {
        return EstrategiaResponse.de(service.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public EstrategiaResponse atualizar(@PathVariable Long id,
            @Valid @RequestBody AtualizarEstrategiaRequest request) {
        return EstrategiaResponse.de(service.atualizar(id, request));
    }
}
