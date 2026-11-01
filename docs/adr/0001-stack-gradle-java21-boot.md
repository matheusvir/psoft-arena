# ADR-0001 - Stack Gradle, Java 21 e Spring Boot 3.5.x

- Status: Aceito
- Data: 2026-10-31

## Contexto

A disciplina ja usa Spring Boot com Gradle Groovy no projeto de referencia
(`atv-springboot/biblioteca`). O time tem cinco pessoas, branches por user story e uma
entrega por etapa; trocar de build ou de versao de linguagem adicionaria conversao sem
retorno.

## Decisao

Usar Gradle com Groovy DSL, wrapper commitado, Java 21 e Spring Boot 3.5.6, com as
dependencias web, JPA, validation, Flyway, H2 e springdoc.

## Alternativas

- **Maven**: stack conhecida no mercado, mas divergiria do projeto da disciplina e exigiria
  reescrever build e comandos do time. Descartado.
- **Java 17**: reduziria exigencia de runtime, mas o ambiente e a disciplina ja usam 21.
  Descartado.
- **Gradle Kotlin DSL**: melhor tipagem no build, porem o time ja escreve Groovy e o ganho
  nao paga a conversao. Descartado.

## Consequencias

- Comandos unicos: `./gradlew build -x test` e `./gradlew test`.
- O wrapper fixa a versao do Gradle e evita "funciona na minha maquina".
- Projeto single-module; se surgir necessidade de modulo separado, sera decisao nova.
- Atualizacao de Boot fica dentro da linha 3.5.x durante a etapa 1.
