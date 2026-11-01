# ADR-0004 - Status alterado por comando, nunca por PUT

- Status: Aceito
- Data: 2026-10-31

## Contexto

`Problema` e `Turma` tem estado (`ATIVO`/`INATIVO`, `ATIVA`/`ENCERRADA`). Se `status`
fosse campo do `PUT`, qualquer edicao poderia reativar um problema desativado ou reabrir
uma turma encerrada, e a transicao ficaria invisivel no contrato.

## Decisao

Status e estado de regra de negocio, sem setter publico. A mudanca acontece por metodos
de dominio (`desativar()`, `encerrar()`) acionados por comandos HTTP explicitos:

- `PATCH /api/v1/problemas/{id}/desativar` -> `204`;
- `PATCH /api/v1/turmas/{id}/encerrar` -> `204`, com `409` para violacao de regra.

`status` nao faz parte do DTO de entrada do `PUT`.

## Alternativas

- **Status no `PUT`**: um request resolve tudo, mas permite transicao invalida e reativacao
  acidental; o efeito nao e auditavel. Descartado.
- **`PATCH` generico com `{"status": "INATIVO"}`**: menos rotas, porem aceita qualquer valor
  e empurra a validacao de transicao para o payload. Descartado.
- **Setter publico + validacao no service**: funciona, mas deixa a porta aberta para outro
  caminho alterar status sem regra. Descartado.

## Consequencias

- `PUT` e idempotente na edicao de atributos e nao mexe em estado.
- Cada nova transicao exige metodo de dominio e rota propria; nao ha reativacao na etapa 1.
- `409` passa a ser esperado em comando de encerrar, alem dos casos de duplicata e FK.
- Testes de `PUT` devem provar que status permanece inalterado.
