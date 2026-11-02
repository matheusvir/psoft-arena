# Contribuindo com o PSoft Arena

Fluxo de trabalho da etapa 1. Vale para todas as USs (`us1`..`us5`) e para o scaffold compartilhado.

## Antes de comecar

1. Clone o repositorio privado:

   ```bash
   git clone https://github.com/matheusvir/psoft-arena.git
   cd psoft-arena
   ```

2. Instale o JDK 21 e confirme:

   ```bash
   java -version
   ```

3. Rode a suite antes de escrever codigo:

   ```bash
   ./gradlew test
   ```

   Se falhar na sua maquina, resolva antes de abrir branch.

4. Leia, nesta ordem:
   - `README.md`: status, contrato HTTP e build.
   - `docs/etapa1.md`: decisoes da etapa.
   - Issue da sua US e das USs que ela consome: requisitos e responsaveis.
   - `docs/api/contrato-http.md`: rotas, payloads e respostas.
   - `docs/adr/`: decisoes de arquitetura, schema e contratos.

## Branches

- `main`: versao entregue de cada etapa. **Sem commit direto.**
- `projeto`: integracao da etapa corrente. **Sem commit direto**, exceto o scaffold inicial.
- `feat/us<N>-<slug>`: uma branch por user story, criada a partir de `projeto` **atualizado**.
  Formato: prefixo `feat/` + `us` + numero da US + slug curto do conteúdo, tudo minusculo com hifen.
  Exemplos: `feat/us1-cadastro-problemas`, `feat/us3-estrategias-avaliacao`,
  `feat/us5-associacao-turma`. Correções usam `fix/us<N>-<slug>`; docs, `docs/<slug>`.

  ```bash
  git switch projeto && git pull --ff-only
  git switch -c feat/us3-estrategias-avaliacao
  ```

O scaffold foi commitado direto em `projeto` antes das branches existirem. Isso e excecao historica autorizada (ver `docs/etapas/etapa1.md`), nao um padrao a repetir.

## Pull requests

- `feat/us*` -> `projeto`: PR normal, com o **revisor do ciclo** designado.
- `projeto` -> `main`: **so no fechamento da etapa**, junto com a tag `etapa1`.
- **Sem squash.** Mantenha os commits da branch; o merge commit preserva o historico.
- **O autor nao mergeia o proprio PR.** Quem aprova e mergeia e o revisor designado.
- PR pequeno e focado na US. Descreva o que muda e por que, e linke a issue.
- Se a branch ficou para tras, faca rebase em `projeto` (nao merge de `projeto` na branch):

  ```bash
  git fetch origin
  git rebase origin/projeto
  ```

### Revisores por US (etapa 1)

| US | Autor | Revisor |
|---|---|---|
| US1 Problemas | Lucaslg7 | @RailtonDantas |
| US2 Estudantes | RailtonDantas | @matheusvir |
| US3 Estrategias | jpereira-Dev | @Lucaslg7 |
| US4 Turmas + professor minimo | ManoelNetto26 | @jpereira-Dev |
| US5 Associacao estudante-turma | matheusvir | @ManoelNetto26 |

## Commits

Use [Conventional Commits](https://www.conventionalcommits.org/), escopo = pacote/recurso:

```text
feat(problema): adiciona PATCH /problemas/{id}/desativar
fix(turma): impede matricula em turma encerrada
test(estudante): cobre 409 de email duplicado
```

- `feat`, `fix`, `test`, `refactor`, `docs`, `build`, `chore`.
- Escopo em portugues, igual ao pacote (`problema`, `estudante`, `estrategia`, `turma`, `professor`, `shared`).
- Mensagem no imperativo e sem "wip"/"ajustes".

## Mudanca de contrato

Alteracao em rota, payload, status ou erro exige, **no mesmo PR**:

1. Requisitos da US documentados na issue e no PR.
2. Doc de contrato atualizado (`docs/api/contrato-http.md`).
3. Teste MockMvc cobrindo o novo comportamento (sucesso + erros do contrato).

PR sem os tres itens nao passa na revisao. Contrato fixado no scaffold (rotas `/api/v1`, JSON em portugues, `201` + `Location`, `204` sem corpo, `ProblemDetail` em `400`/`404`/`409`) so muda com justificativa explicita no PR.

## Mudanca de schema

- Crie **sempre** uma migration nova (`V2__...sql`, `V3__...sql`); **nunca edite** uma migration ja aplicada (Flyway valida o checksum e o banco de quem ja rodou quebra).
- O scaffold entrega so a `V1`; branches de US nao criam migration propria na etapa 1.
  Excecao aprovada uma unica vez: V2 da US2 (ADR-0009).
- Antes de abrir o PR, faca rebase em `projeto` e confira numeracao/ordem das migrations.
- Rode o teste de Flyway/migracao junto com a suite: `./gradlew test` (H2 em memoria) e suba a aplicacao uma vez para aplicar no H2 de arquivo.
- So SQL ANSI (compatibilidade H2/Postgres). Sem JSONB, window function ou extensao.

## Planejamento e revisao de qualidade

- Antes de implementar, defina um plano curto com escopo, responsabilidades e evidencias de verificacao; revise o plano para evitar complexidade e acoplamento desnecessarios.
- Em toda implementacao, revise o diff para identificar e corrigir code smells: duplicacao evitavel, nomes pouco claros, responsabilidades misturadas, metodos extensos e abstracoes sem necessidade concreta.
- Mantenha testes independentes e diagnosticaveis. Combinacoes de entradas e resultados devem usar testes parametrizados com nomes descritivos, em vez de loops que agrupam varias falhas em um unico caso.
- Para endpoints REST, confira o OpenAPI gerado contra o contrato e o comportamento real: status de sucesso/erro, headers, tipos de midia, schemas e ausencia de corpo quando aplicavel.
- Testes verdes nao comprovam ausencia de code smells; registre na descricao do PR o que foi revisado e quaisquer limitacoes conhecidas.

## Definicao de pronto

Um PR so e mergeavel quando:

- `./gradlew test` verde.
- Testes de integracao/MockMvc cobrindo sucesso e erros do contrato da US.
- Plano e diff revisados quanto a code smells; contrato OpenAPI e isolamento dos cenarios verificados quando aplicavel.
- Contrato/spec/doc atualizados quando aplicavel.
- Revisor do ciclo aprovou, e o **revisor** fez o merge (sem squash).
- Sem codigo de debug, `TODO` solto ou arquivo gerado commitado.
- Issue da US movida para Done no Project.
