# ADR-0005 - TurmaEstudante como entidade explicita

- Status: Aceito
- Data: 2026-10-31

## Contexto

A US5 associa estudante a turma. O vinculo precisa carregar a data de associacao, ser
unico por par e, no futuro, respeitar turma encerrada. `@ManyToMany` esconde a tabela de
juncao e dificulta anexar atributos.

## Decisao

Modelar `TurmaEstudante` como entidade propria, com `id`, `turma_id`, `estudante_id` e
`associado_em`, `UNIQUE(turma_id, estudante_id)` e FKs com `ON DELETE RESTRICT`. A
remocao do vinculo e hard delete na etapa 1.

## Alternativas

- **`@ManyToMany` entre Turma e Estudante**: menos codigo, mas sem `associadoEm`, sem
  entidade para evoluir e com semantica de colecao que esconde regras. Descartado.
- **Soft delete desde ja (`removido_em`)**: preservaria historico, porem ninguem consome
  esse historico na etapa 1 e toda consulta precisaria de filtro. Fica como hedge.
- **Vinculo como atributo da Turma**: quebraria normalizacao e duplicaria estudante.

## Consequencias

- Associacao tem identidade, data e constraints explicitas no banco.
- Consultas e comandos tratam o vinculo como recurso (`/turmas/{id}/estudantes`), com
  `404` para turma ou estudante inexistente e `409` para par duplicado.
- Hard delete remove o vinculo de fato; se US14/17 exigir historico de matricula, entra
  `removido_em` nullable mais filtro em migration futura.
- `ON DELETE RESTRICT` impede apagar turma ou estudante com vinculo, o que hoje retorna
  `409`.
