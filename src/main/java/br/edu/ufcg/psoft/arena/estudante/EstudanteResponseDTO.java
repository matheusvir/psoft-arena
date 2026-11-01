package br.edu.ufcg.psoft.arena.estudante;

public class EstudanteResponseDTO {

    private final Long id;
    private final String nome;
    private final String matricula;
    private final String email;

    public EstudanteResponseDTO(Long id, String nome, String matricula, String email) {
        this.id = id;
        this.nome = nome;
        this.matricula = matricula;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getMatricula() {
        return matricula;
    }

    public String getEmail() {
        return email;
    }
}
