# ADR-0003 - Package-by-feature e contrato apenas por DTO

- Status: Aceito
- Data: 2026-10-31

## Contexto

O projeto cresce por user story e deve chegar a US6+ com avaliacao, submissao e catalogo
de LLM. Organizar por camada (`controller/`, `service/`, `repository/`) concentraria
todos os controllers e services em poucas pastas. Expor entidade JPA no controller
tambem acoplaria o JSON ao mapeamento e arriscaria serializar proxy ou campo interno.

## Decisao

Organizar o codigo por feature (`problema/`, `estudante/`, `estrategia/`, `turma/`,
`professor/`, `shared/`). Cada feature contem controller, service, repository, entidade e
DTOs. Controller recebe e devolve somente DTO; o mapeamento fica no service/assembler.

## Alternativas

- **Layer-first**: familiar, mas vira pasta unica com 15 controllers e 15 services ja na
  etapa 3. Descartado.
- **Expor entidade**: menos codigo de mapeamento, porem o contrato muda junto com o schema,
  `status` e relacoes vazam e `@Data`/proxy criam bugs de igualdade e serializacao.
  Descartado.
- **MapStruct**: reduziria boilerplate, mas adiciona processamento de anotacao e mais uma
  dependencia para um mapeamento pequeno. Mapeamento manual resolve.

## Consequencias

- Mudanca de feature fica localizada; review por US enxerga o pacote inteiro.
- Ha custo de mapeamento manual e risco de DTO esquecido; testes de controller cobrem isso.
- DTO de entrada nunca declara `id` nem `status`; DTO de saida declara o que o contrato
  expoe, independente da entidade.
- `shared/` guarda apenas o que e realmente transversal: `BaseEntity`, excecoes, handler e
  configuracao de OpenAPI.
