# PSoft Arena

Plataforma para submissao e avaliacao de solucoes de problemas de projeto de software
orientado a objetos. O professor cadastra problemas, estrategias de avaliacao, turmas e
modelos de LLM; os estudantes submetem solucoes em Java e acompanham o processamento e os
resultados. A avaliacao e automatica e nao executa o codigo submetido: usa analise estatica
e APIs externas de LLM.

## Status

Etapa 1 em andamento: US1-US5 implementadas; integracao final de `projeto` em `main`
pendente. Entrega e decisoes em [docs/etapas/etapa1.md](docs/etapas/etapa1.md).

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
    class Estrategia {
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
    Estrategia --> TipoEstrategia
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

## US4 - Professor e turma

A US4 implementa o cadastro minimo de professor (criacao e consultas) e o cadastro,
consulta, edicao e encerramento de turmas, conforme o contrato HTTP. Professor deve
existir antes de cadastrar turma. Turmas nascem `ATIVA`; encerrar e uma transicao final,
com historico preservado. Associacoes de estudantes sao implementadas na US5.

## US5 - Associacao de estudantes

A US5 permite associar, consultar e remover estudantes de uma turma. As rotas ficam em
`/api/v1/turmas/{id}/estudantes`: `POST` recebe apenas `estudanteId` e responde
`201 + Location`; `GET` lista os vinculos; `GET /{estudanteId}` consulta um vinculo; e
`DELETE /{estudanteId}` remove o vinculo com `204`. O `Location` do POST aponta para a
rota de consulta do vinculo.

Somente turma `ATIVA` aceita nova associacao ou remocao; turma `ENCERRADA` retorna `409`.
O par turma-estudante e unico, mas o mesmo estudante pode participar de varias turmas.
A remocao e hard delete do vinculo, preservando estudante e turma. Nao ha migration nova:
a V1 ja contempla `turma_estudante`.

## Documentacao

- [CONTRIBUTING.md](CONTRIBUTING.md): branches, ordem de merge, revisores e checklist de PR.
- [docs/api/contrato-http.md](docs/api/contrato-http.md): contrato HTTP da etapa 1.
- [docs/etapas/etapa1.md](docs/etapas/etapa1.md): entrega, participacao e decisoes da etapa 1.
- [docs/adr/](docs/adr/): decisoes arquiteturais registradas.
- [especificacao_psoft_arena.pdf](especificacao_psoft_arena.pdf): especificacao da disciplina.
