# ADR-0006 - V1 unica no scaffold e migrations depois da etapa

- Status: Aceito
- Data: 2026-10-31

## Contexto

US1, US2 e US3 sao desenvolvidas em paralelo a partir de `projeto`. Se cada branch criasse
sua propria migration, o merge produziria versoes repetidas (V1/V2/V3 concorrentes) e
conflito no historico do Flyway.

## Decisao

O scaffold entrega uma unica migration, `V1__esquema_etapa1.sql`, cobrindo todas as
tabelas da etapa. Branches de US na etapa 1 alteram apenas Java; schema novo so entra
depois do fechamento da etapa, em migrations `V2`, `V3`, ... sequenciais.

## Alternativas

- **Uma migration por US**: cada branch fica autonoma, mas o merge exige renumerar e
  reaplicar; risco alto com tres branches em paralelo. Descartado.
- **`ddl-auto: update` e sem Flyway**: dispensa coordenacao, porem perde versionamento e
  reprodutibilidade. Descartado (ver ADR-0002).
- **Migrations por feature em pastas separadas**: o Flyway nao isola historico por pasta
  do jeito que o time precisaria. Descartado.

## Consequencias

- Merge de US1-US5 sem conflito de migration.
- Ajuste de schema descoberto durante a etapa 1 e feito na V1 pelo dono do scaffold, com
  o time avisado, antes do fechamento.
- Depois da tag `etapa1`, migration aplicada e imutavel; correcao entra como nova versao.
- Toda mudanca de schema passa a ser revisada como parte do PR da feature que a motivou.
