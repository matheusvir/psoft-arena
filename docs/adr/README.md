# ADRs - PSoft Arena

Registro das decisoes de arquitetura da etapa 1. Cada ADR e curto e responde a uma
decisao; o "porque" completo fica no proprio arquivo, nao no historico de PR.

## Indice

| ADR | Decisao | Status |
|---|---|---|
| [0001](0001-stack-gradle-java21-boot.md) | Gradle Groovy + Java 21 + Spring Boot 3.5.x | Aceito |
| [0002](0002-persistencia-h2-flyway.md) | H2 em arquivo + Flyway + `ddl-auto: validate` | Aceito |
| [0003](0003-package-by-feature-dto.md) | Package-by-feature e contrato so por DTO | Aceito |
| [0004](0004-status-por-comando.md) | Status alterado por comando, nunca por `PUT` | Aceito |
| [0005](0005-turma-estudante-explicita.md) | `TurmaEstudante` como entidade explicita | Aceito |
| [0006](0006-v1-unica-e-migrations.md) | V1 unica no scaffold; migrations depois da etapa | Aceito |
| [0007](0007-rotas-planas-sem-auth.md) | Rotas planas e sem autenticacao na etapa 1 | Aceito |
| [0008](0008-professor-minimo-na-us4.md) | Professor minimo dentro da US4 | Aceito |
| [0009](0009-normalizacao-email-unico-estudante.md) | Normalizacao e e-mail unico de estudante; excecao ao ADR-0006 | Aceito |

## Template

```markdown
# ADR-XXXX - Titulo curto no imperativo

- Status: Proposto | Aceito | Rejeitado | Substituido por ADR-YYYY
- Data: AAAA-MM-DD

## Contexto

Situacao, forcas e restricoes que tornam a decisao necessaria.

## Decisao

O que foi decidido, em uma ou duas frases verificaveis.

## Alternativas

- **Alternativa A**: descricao; por que nao foi escolhida.
- **Alternativa B**: descricao; por que nao foi escolhida.

## Consequencias

Efeitos positivos e negativos, incluindo divida tecnica e o que passa a ser proibido.
```

## Regra de imutabilidade

ADR aceito nao e reescrito. Correcao, complemento ou reversao entra em um novo ADR que
cita o anterior. A unica edicao permitida em um ADR aceito e o campo `Status`, trocado
para `Substituido por ADR-YYYY` (ou `Obsoleto`) quando o substituto for aceito; o corpo
e as alternativas permanecem como estavam. O indice acima e atualizado junto.

## Quando criar um ADR

- decisao que atravessa mais de uma US ou dificil de reverter;
- convencao que as branches precisam seguir para nao divergir;
- escolha entre alternativas plausiveis, com custo registrado.

Detalhe de implementacao local, nome de metodo ou ajuste de teste nao viram ADR.
