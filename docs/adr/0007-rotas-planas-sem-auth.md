# ADR-0007 - Rotas planas e sem autenticacao na etapa 1

- Status: Aceito
- Data: 2026-10-31

## Contexto

Ainda nao ha modelo de identidade nem login. Ainda assim, problemas, estrategias e turmas
pertencem, no dominio final, a um professor. Aninhar tudo em
`/professores/{professorId}/...` agora exigiria inventar escopo antes de existir
autenticacao.

## Decisao

Expor recursos em rotas planas sob `/api/v1` (`/problemas`, `/estrategias`, `/turmas`,
`/estudantes`, `/professores`), sem autenticacao e sem filtro por professor na etapa 1.
O escopo por professor entra na etapa 3 como query param `professorId`, junto com auth.

## Alternativas

- **Rotas aninhadas por professor**: expressa o dominio, mas obriga toda US a repetir o
  prefixo e cria um parametro que ninguem valida. Descartado.
- **Auth basica agora**: anteciparia etapa 3, mas atrasaria US1-US5 e nao ha requisito de
  identidade fechado. Descartado.
- **Rotas planas com `professorId` obrigatorio no body**: meio caminho, sem autenticacao
  para provar o dono; viraria dado decorativo. Descartado.

## Consequencias

- Contrato das US1-US5 fica curto e estavel.
- Qualquer cliente acessa qualquer recurso; aceitavel enquanto o repositorio e privado e
  o ambiente e de disciplina.
- Adicionar `professorId` como filtro opcional na etapa 3 e compativel; exigir auth sera
  mudanca de contrato e precisa de nova decisao.
- A divida esta registrada em `docs/etapas/etapa1.md` (pendencias) e no design do scaffold.
