# PSoft Arena

Plataforma para submissao e avaliacao de solucoes de problemas de projeto de software
orientado a objetos. O professor cadastra problemas, estrategias de avaliacao, turmas e
modelos de LLM; os estudantes submetem solucoes em Java e acompanham o processamento e os
resultados. A avaliacao e automatica e nao executa o codigo submetido: usa analise estatica
e APIs externas de LLM.

## Status

Etapa 1 em andamento: scaffold Spring Boot (build, `shared/`, migration V1) em `projeto` e
US1-US5 nas branches proprias. Entrega e decisoes em [docs/etapas/etapa1.md](docs/etapas/etapa1.md).

## Modelagem

Classes de dominio da etapa 1. Sem controller, service ou repository. Detalhe de banco
em Quickstart / Persistencia (V1).

```mermaid
classDiagram
    class Professor {
        String nome
        String email
    }
    class Problema {
        String titulo
        String descricao
        String requisitosFuncionais
        String requisitosProjeto
        StatusProblema status
        desativar()
    }
    class Estudante {
        String nome
        String matricula
        String email
    }
    class EstrategiaAvaliacao {
        String nome
        TipoEstrategia tipo
        String descricao
        String prompt
    }
    class Turma {
        String nome
        String codigo
        String periodoLetivo
        StatusTurma status
        encerrar()
        adicionarEstudante()
        removerEstudante()
    }
    class StatusProblema {
        <<enumeration>>
        ATIVO
        INATIVO
    }
    class StatusTurma {
        <<enumeration>>
        ATIVA
        ENCERRADA
    }
    class TipoEstrategia {
        <<enumeration>>
        CODE_SMELLS
        ACOPLAMENTO_COESAO_PRINCIPIOS
        PADROES_DE_PROJETO
    }

    Professor "1" --> "0..*" Turma : responsavel
    Turma "0..*" --> "0..*" Estudante : participa
    Problema --> StatusProblema
    Turma --> StatusTurma
    EstrategiaAvaliacao --> TipoEstrategia
```

Problema nasce `ATIVO` e passa a `INATIVO` so por `desativar()`; turma nasce `ATIVA`
e passa a `ENCERRADA` so por `encerrar()`; estudante-turma exige turma ativa e sem duplicata.

## Quickstart

JDK 21; o wrapper Gradle ja vem no repositorio.

```bash
./gradlew test      # testes com H2 em memoria
./gradlew bootRun   # aplicacao em http://localhost:8080
```

Banco H2 em arquivo em `./data/arena`; OpenAPI/Swagger em `/v3/api-docs` e `/swagger-ui.html`.
Persistencia (V1, em `src/main/resources/db/migration/V1__esquema_etapa1.sql`): entidades com
`id` gerado (`BaseEntity`); associacao turma-estudante em tabela `turma_estudante` com
`associado_em`. Uniques: `estudante.matricula`, `professor.email`,
`turma(codigo, periodo_letivo)` e `turma_estudante(turma_id, estudante_id)`.
`turma.professor_id` e as duas FKs de `turma_estudante` usam `ON DELETE RESTRICT`.

## Estrutura

```text
br.edu.ufcg.psoft.arena
├── problema/  estudante/  estrategia/  turma/  professor/   # feature: entidade, repository, service, controller, DTO
└── shared/                                                   # BaseEntity, exceptions, OpenApiConfig, migration V1
```

## Contrato HTTP

Tudo sob `/api/v1`, com JSON em portugues:

- criacao responde `201` + `Location`; remocao e comando respondem `204` sem corpo;
- erros usam `ProblemDetail`: `400` validacao, `404` inexistente, `409` regra/duplicata/FK;
- `PATCH .../desativar` e `PATCH .../encerrar` sao comandos explicitos, nao campos do `PUT`.

Tabela completa em [docs/api/contrato-http.md](docs/api/contrato-http.md).

## Documentacao

- [CONTRIBUTING.md](CONTRIBUTING.md): branches, ordem de merge, revisores e checklist de PR.
- [docs/api/contrato-http.md](docs/api/contrato-http.md): contrato HTTP da etapa 1.
- [docs/etapas/etapa1.md](docs/etapas/etapa1.md): entrega, participacao e decisoes da etapa 1.
- [docs/adr/](docs/adr/): decisoes arquiteturais registradas.
- [especificacao_psoft_arena.pdf](especificacao_psoft_arena.pdf): especificacao da disciplina.
