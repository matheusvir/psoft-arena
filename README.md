# PSoft Arena

Plataforma para submissao e avaliacao de solucoes de problemas de projeto de software
orientado a objetos.

O professor cadastra problemas, estrategias de avaliacao, turmas, atividades e modelos de
LLM. Os estudantes submetem solucoes em Java e acompanham o processamento e os resultados
das avaliacoes. A avaliacao e feita automaticamente, sem executar o codigo submetido, por
meio de analise estatica e de APIs externas de LLM.

## Status

Etapa 1 em construcao: base Spring Boot privada com scaffold compartilhado (build, `shared/`,
migration V1) e as US1-US5 implementadas em branches proprias.

## Fluxo de branches

- `main`: versao entregue de cada etapa.
- `projeto`: integracao da etapa corrente; recebe o scaffold e os PRs das USs.
- `us1`..`us5`: uma branch por user story, criada a partir de `projeto`.

Ordem de merge: scaffold -> {US1, US2, US3} em paralelo -> US4 (professor minimo + turma)
-> US5. O fechamento da etapa1 e feito por PR `projeto` -> `main` (sem squash) + tag
`etapa1`.

## Revisores

| US | Responsavel | Revisor |
|---|---|---|
| US1 Problemas | Lucaslg7 | @RailtonDantas |
| US2 Estudantes | RailtonDantas | @matheusvir |
| US3 Estrategias | jpereira-Dev | @Lucaslg7 |
| US4 Turmas + professor minimo | ManoelNetto26 | @jpereira-Dev |
| US5 Associacao estudante-turma | matheusvir | @ManoelNetto26 |

## Stack

Java 21, Spring Boot 3.5.6, Gradle (Groovy), Spring Data JPA, Validation, Flyway, H2
(arquivo no runtime, memoria nos testes), springdoc-openapi 2.8.9.

## Contrato HTTP (etapa 1)

Todos os recursos ficam sob `/api/v1`, com JSON em portugues conforme a especificacao.
`201` retorna `Location`, `204` nao tem corpo e erros usam `ProblemDetail`
(`400` validacao, `404` inexistente, `409` regra de negocio/duplicata/FK).

| Recurso | Verbo | Rota | Sucesso | Erros |
|---|---|---|---|---|
| Problemas | POST | `/api/v1/problemas` | 201 + Location | 400 |
| Problemas | GET | `/api/v1/problemas` | 200 | - |
| Problemas | GET | `/api/v1/problemas/{id}` | 200 | 404 |
| Problemas | PUT | `/api/v1/problemas/{id}` | 200 | 400, 404 |
| Problemas | PATCH | `/api/v1/problemas/{id}/desativar` | 204 | 404 |
| Problemas | DELETE | `/api/v1/problemas/{id}` | 204 | 404, 409 |
| Estudantes | POST | `/api/v1/estudantes` | 201 + Location | 400, 409 |
| Estudantes | GET | `/api/v1/estudantes` | 200 | - |
| Estudantes | GET | `/api/v1/estudantes/{id}` | 200 | 404 |
| Estudantes | PUT | `/api/v1/estudantes/{id}` | 200 | 400, 404, 409 |
| Estrategias | POST | `/api/v1/estrategias` | 201 + Location | 400 |
| Estrategias | GET | `/api/v1/estrategias` | 200 | - |
| Estrategias | GET | `/api/v1/estrategias/{id}` | 200 | 404 |
| Estrategias | PUT | `/api/v1/estrategias/{id}` | 200 | 400, 404 |
| Turmas | POST | `/api/v1/turmas` | 201 + Location | 400, 404, 409 |
| Turmas | GET | `/api/v1/turmas` | 200 | - |
| Turmas | GET | `/api/v1/turmas/{id}` | 200 | 404 |
| Turmas | PUT | `/api/v1/turmas/{id}` | 200 | 400, 404, 409 |
| Turmas | PATCH | `/api/v1/turmas/{id}/encerrar` | 204 | 404, 409 |
| TurmaEstudantes | POST | `/api/v1/turmas/{id}/estudantes` | 201 + Location | 400, 404, 409 |
| TurmaEstudantes | GET | `/api/v1/turmas/{id}/estudantes` | 200 | 404 |
| TurmaEstudantes | DELETE | `/api/v1/turmas/{id}/estudantes/{estudanteId}` | 204 | 404, 409 |
| Professores | POST | `/api/v1/professores` | 201 + Location | 400, 409 |
| Professores | GET | `/api/v1/professores` | 200 | - |
| Professores | GET | `/api/v1/professores/{id}` | 200 | 404 |

`PATCH` de desativar/encerrar e comando explicito: status nao e campo do `PUT`.

## Documentacao

- `docs/etapa1.md`: resumo da etapa, participacao, decisoes e diagrama de classes.
- OpenAPI/Swagger disponivel em `/v3/api-docs` e `/swagger-ui.html` com a aplicacao no ar.

## Build e testes

```bash
./gradlew build -x test
./gradlew test
```

O banco H2 de arquivo fica em `./data/arena`; os testes usam H2 em memoria.
