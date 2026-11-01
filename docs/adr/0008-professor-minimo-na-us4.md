# ADR-0008 - Professor minimo dentro da US4

- Status: Aceito
- Data: 2026-10-31

## Contexto

`turma.professor_id` e `NOT NULL` e a US4 precisa criar turma. Sem um recurso de
professor, a US4 nao fecha: seria preciso inventar id, afrouxar a FK ou criar um CRUD
completo que ninguem pediu.

## Decisao

Incluir na US4 (ManoelNetto26) um professor minimo: `nome` e `email`, `POST` e `GET`
lista/por id, `email` unico e FK de turma com `ON DELETE RESTRICT`. Sem `PUT`, sem
`DELETE` e sem issue/task separada; o professor e implementado antes da turma na mesma
branch.

## Alternativas

- **CRUD completo de professor**: anteciparia etapas futuras, mas amplia escopo da US4 e
  cria endpoints sem requisito. Descartado.
- **Recurso separado com dono proprio**: dividiria responsabilidade, porem o time ja tem
  cinco USs e a FK e bloqueante para a US4. Descartado.
- **`professor_id` nullable na etapa 1**: destravaria a turma, mas enfraqueceria o modelo e
  exigiria migration corretiva. Descartado.

## Consequencias

- US4 entrega professor e turma juntos, sem dependencia externa.
- Professor nao e pontuado como US; e recurso de suporte, com superficie minima.
- Nao ha edicao nem remocao de professor na etapa 1; correcao de dado fica para etapa
  futura ou intervencao manual.
- `ON DELETE RESTRICT` preserva turmas existentes e transforma tentativa de remocao em
  `409`, caso o endpoint venha a existir.
