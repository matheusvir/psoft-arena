# ADR-0009 - Normalize estudantes e garanta e-mail unico

- Status: Aceito
- Data: 2026-11-01

## Contexto

A US2 foi ampliada por solicitacao do usuario: trim nos tres campos e e-mail unico
sem distinguir caixa. A coluna `email` da V1 nao possui unicidade. O usuario autorizou
explicitamente a excecao ao ADR-0006 para uma nova migration durante a etapa 1.

## Decisao

Preservar a V1 e adicionar V2 Java, proxima versao livre neste checkout, coordenando
a numeracao na integracao. Normalizar nome e matricula com trim; normalizar o proprio
email com trim e lowercase independente de locale. Adicionar UNIQUE em `email`, sem
coluna auxiliar. DTOs e dominio usam a mesma normalizacao; a migration mantem seu
contrato historico independente dos DTOs atuais. Esta e uma excecao especifica ao
ADR-0006, nao uma autorizacao geral para outras branches criarem migrations.

## Alternativas

- **Alterar V1**: descartado para preservar bancos que ja aplicaram o scaffold.
- **Verificar apenas no service**: descartado porque nao impede duplicatas concorrentes.
- **Coluna normalizada adicional**: desnecessaria; o proprio email armazena o valor canonico.

## Consequencias

Pre-validar todos os dados antes de alterar o banco. Colisoes e valores invalidos
interrompem o upgrade com os IDs envolvidos, sem apagar ou mesclar estudantes.
Fixtures devem usar emails normalizados e distintos. Fazer backup antes do upgrade
de banco persistente; o trim e a conversao de caixa nao sao reversiveis sem backup.
DDL pode nao ser transacional: falhas exigem inspecao e recuperacao controlada, nao
edicao da migration aplicada. No rollback da aplicacao, verificar compatibilidade
dos escritores com as novas regras. Autenticacao continua fora desta etapa.
