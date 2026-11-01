# ADR-0002 - Persistencia com H2 em arquivo, Flyway e schema validado

- Status: Aceito
- Data: 2026-10-31

## Contexto

A etapa 1 precisa de banco funcional sem infraestrutura externa, com schema versionado e
sem divergencia entre o que o codigo espera e o que existe no banco. O time nao tem
Docker nem servico gerenciado no fluxo atual.

## Decisao

Usar H2 em arquivo (`./data/arena`) no runtime e H2 em memoria nos testes. O schema vem
de migrations Flyway, com `spring.jpa.hibernate.ddl-auto: validate` e
`spring.jpa.open-in-view: false`.

## Alternativas

- **PostgreSQL desde a etapa 1**: mais proximo de producao, mas exigiria Docker ou servico
  externo sem retorno imediato. Fica como profile futuro.
- **`ddl-auto: update`**: menos arquivos, porem o schema passaria a depender do mapeamento
  e divergencias ficariam silenciosas. Descartado.
- **Banco em memoria no runtime**: simples, mas perde dados a cada restart e nao permite
  inspecionar o estado entre execucoes.

## Consequencias

- Zero infraestrutura para rodar e testar; `validate` falha cedo se entidade e migration
  divergirem.
- SQL restrito ao que H2 e Postgres aceitam (sem JSONB, window functions ou extensoes),
  para a troca de profile nao virar reescrita.
- `open-in-view: false` obriga o carregamento explicito das associacoes no service.
- Migrations passam a ser o unico caminho para mudar schema (ver ADR-0006).
