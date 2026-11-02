# Contrato HTTP - Etapa 1

Contrato fixado para as US1-US5. O objetivo e impedir que as branches das user stories
divirjam em rota, verbo ou codigo de status. Este documento e a referencia escrita; o
schema executavel (campos, tipos, obrigatoriedade) e o OpenAPI gerado em runtime.

## Convencoes gerais

- Base path: `/api/v1`. Rotas planas, sem aninhamento por professor (ver ADR-0007).
- JSON com nomes em portugues, em `camelCase`.
- Criacao bem-sucedida: `201 Created` com header `Location` apontando para o recurso.
- Remocao e comandos sem corpo de resposta: `204 No Content`.
- Erros: `ProblemDetail` (RFC 9457), com `400`, `404` e `409` descritos abaixo.
- Sem autenticacao e sem paginacao na etapa 1.
- Controller recebe e devolve apenas DTOs; entidade JPA nunca e serializada (ver ADR-0003).
- `status` nao e campo de `PUT`; mudanca de status so por comando `PATCH` (ver ADR-0004).

## Tabela de endpoints

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

Leitura da coluna de erros:

- `400`: payload invalido por Bean Validation ou erro de sintaxe/conversao da requisicao,
  como JSON malformado, corpo obrigatorio ausente ou parametro de rota cujo tipo nao pode
  ser convertido. Apenas erros de Bean Validation incluem a propriedade `erros`.
- `404`: recurso do path inexistente quando o id tem formato valido; em rotas aninhadas,
  qualquer um dos ids validos do path.
- `409`: duplicata (email, matricula, `codigo`+`periodoLetivo`, par turma-estudante),
  violacao de regra de negocio (ex.: encerrar turma) ou FK bloqueando a operacao
  (`ON DELETE RESTRICT`).

A coluna lista erros de operacao por endpoint. Erros sintaticos transversais usam `400`
mesmo em rotas cuja tabela lista apenas `404`; um id malformado nao e tratado como recurso
inexistente. Esses erros tambem usam `ProblemDetail`, sem expor a excecao de parsing.

## Campos de entrada (derivados da V1)

Os DTOs de entrada refletem as colunas da migration, sem `id`, sem `status` e com os
nomes convertidos para `camelCase`. O OpenAPI em runtime e a fonte de verdade para
tipos, tamanhos e obrigatoriedade.

| Recurso | Campos de entrada |
|---|---|
| Problema | `titulo`, `descricao`, `requisitosFuncionais`, `requisitosProjeto` |
| Estudante | `nome`, `matricula`, `email` |
| Estrategia | `nome`, `tipo`, `descricao`, `prompt` |
| Turma | `nome`, `codigo`, `periodoLetivo`, `professorId` |
| TurmaEstudante | `estudanteId` |
| Professor | `nome`, `email` |

`tipo` da estrategia aceita `CODE_SMELLS`, `ACOPLAMENTO_COESAO_PRINCIPIOS` e
`PADROES_DE_PROJETO`. Turma nasce `ATIVA` e problema nasce `ATIVO`; esses valores nao
sao enviados pelo cliente.

### Estudantes (US2)

- POST e PUT exigem `nome` (ate 120 caracteres), `matricula` (ate 20) e `email`
  valido (ate 160), nao nulos e nao brancos.
- Antes de validar ou persistir, aplicar trim nos tres campos e converter email
  para minusculas. Limites sao medidos apos normalizacao. Espacos internos e caixa
  de nome/matricula sao preservados; email e retornado normalizado.
- Matricula e email sao unicos. Email nao distingue maiusculas/minusculas.
  Duplicata retorna `409`, inclusive em requisicoes concorrentes.
- Respostas contem apenas `id`, `nome`, `matricula` e `email`.
- GET lista retorna array sem paginacao e sem ordem garantida; lista vazia retorna `[]`.
- PUT exige os tres campos completos e preserva o ID. Pode alterar matricula e email
  para valores disponiveis ou manter os proprios identificadores, inclusive variando
  caixa/espacos externos do email na entrada. Retorna `200` com dados normalizados;
  `400`/`409` nao alteram nenhum campo e `404` nao cria um novo estudante.
- Unicidade de email e normalizacao sao extensoes aprovadas da US2 original.
  O schema evolui pela V2, sem alterar V1 nem adicionar coluna auxiliar (ADR-0009).

## Status fora do PUT

`PUT` substitui apenas os atributos editaveis do recurso. `status` nao faz parte do DTO
de entrada, entao nao ha como alterar estado por `PUT`, nem por omissao acidental nem
por envio do campo.

Motivo: a transicao de estado e regra de negocio, nao edicao de dados. Concentrar a
mudanca em um comando explicito deixa o efeito auditavel na rota e impede reativacao
por bypass. Na etapa 1 nao existe endpoint de reativacao: `ATIVO -> INATIVO` e
`ATIVA -> ENCERRADA` sao finais.

## Comandos PATCH

| Comando | Rota | Efeito | Respostas |
|---|---|---|---|
| Desativar problema | `PATCH /api/v1/problemas/{id}/desativar` | `ATIVO` -> `INATIVO` | 204; 404 |
| Encerrar turma | `PATCH /api/v1/turmas/{id}/encerrar` | `ATIVA` -> `ENCERRADA` | 204; 404, 409 |

Sao comandos, nao atualizacao parcial generica: a rota nomeia a transicao e nao aceita
corpo. O `409` de encerrar cobre a regra de negocio da turma; `404` cobre id
inexistente. Sem corpo na resposta.

## ProblemDetail

Todo erro usa `application/problem+json` via `ProblemDetail`. Formato base:

```json
{
  "type": "about:blank",
  "title": "Recurso nao encontrado",
  "status": 404,
  "detail": "Problema 42 nao encontrado.",
  "instance": "/api/v1/problemas/42"
}
```

`type` fica em `about:blank` na etapa 1; `detail` e a mensagem legivel e `instance` e
preenchido pelo Spring. O handler compartilhado esta em
`shared/exception/ApiExceptionHandler.java`.

### 400 - validacao

Inclui a propriedade extra `erros`, um mapa `campo -> mensagem`:

```json
{
  "type": "about:blank",
  "title": "Requisicao invalida",
  "status": 400,
  "detail": "Um ou mais campos da requisicao sao invalidos.",
  "instance": "/api/v1/estudantes",
  "erros": {
    "matricula": "nao deve estar em branco",
    "email": "deve ser um endereco de e-mail valido"
  }
}
```

### 404 - inexistente

```json
{
  "type": "about:blank",
  "title": "Recurso nao encontrado",
  "status": 404,
  "detail": "Turma 7 nao encontrada.",
  "instance": "/api/v1/turmas/7"
}
```

### 409 - conflito

Duplicata, regra de negocio e violacao de FK usam o mesmo status, com `title` distinto:

```json
{
  "type": "about:blank",
  "title": "Conflito de dados",
  "status": 409,
  "detail": "Operacao viola uma restricao de integridade dos dados.",
  "instance": "/api/v1/estudantes"
}
```

```json
{
  "type": "about:blank",
  "title": "Regra de negocio violada",
  "status": 409,
  "detail": "Turma ja encerrada.",
  "instance": "/api/v1/turmas/7/encerrar"
}
```

## DTO-only

Controllers nao recebem nem devolvem entidades JPA. O fluxo e
`DTO de entrada -> service -> entidade -> DTO de saida`, com o mapeamento no
service/assembler do proprio pacote de feature.

Consequencias praticas:

- contrato HTTP independente do modelo de persistencia;
- sem serializacao acidental de proxy/lazy;
- `id`, `status` e `associadoEm` aparecem na resposta porque o DTO de saida os declara,
  nao porque a entidade foi exposta.

## OpenAPI em runtime

Nao ha spec estatica versionada. O documento e derivado em runtime dos controllers e
DTOs pelo springdoc-openapi:

- JSON: `/v3/api-docs`
- UI: `/swagger-ui.html`

Metadados (titulo, versao `v1`, descricao) ficam em `shared/config/OpenApiConfig.java`.
Mudanca de contrato implica mudanca no codigo; a documentacao gerada acompanha. Este
arquivo cobre o que o gerador nao expressa bem: convencoes, semantica dos status e a
fronteira entre `PUT` e comandos `PATCH`.
