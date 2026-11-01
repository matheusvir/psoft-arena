package br.edu.ufcg.psoft.arena.problema;

import java.net.URI;
import java.util.List;

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

import br.edu.ufcg.psoft.arena.problema.dto.ProblemaRequest;
import br.edu.ufcg.psoft.arena.problema.dto.ProblemaResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/problemas")
public class ProblemaController {

    private final ProblemaService servico;

    public ProblemaController(ProblemaService servico) {
        this.servico = servico;
    }

    @PostMapping
    public ResponseEntity<ProblemaResponse> criar(@Valid @RequestBody ProblemaRequest requisicao) {
        ProblemaResponse resposta = servico.criar(requisicao);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resposta.id())
                .toUri();
        return ResponseEntity.created(location).body(resposta);
    }

    @GetMapping
    public List<ProblemaResponse> listar() {
        return servico.listar();
    }

    @GetMapping("/{id}")
    public ProblemaResponse buscarPorId(@PathVariable Long id) {
        return servico.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public ProblemaResponse atualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProblemaRequest requisicao) {
        return servico.atualizar(id, requisicao);
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        servico.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        servico.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
