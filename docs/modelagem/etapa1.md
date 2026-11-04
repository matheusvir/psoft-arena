# Modelagem — etapa 1 (US1 a US5)

Snapshot congelado em 03/11/2026. Não editar: correções só factuais, com nota de data.
Diagrama vivo da etapa corrente fica no README.

```mermaid
classDiagram
    class Professor {
        String nome
        String email
    }
    class Problema {
        String titulo
        String descricao
        String requisitosFuncionais
        String requisitosProjeto
        StatusProblema status
        atualizarDados()
        desativar()
        exigirAtivoParaNovaAtividade()
        validarPodeRemover()
    }
    class Estudante {
        String nome
        String matricula
        String email
        atualizarDados()
    }
    class Estrategia {
        String nome
        TipoEstrategia tipo
        String descricao
        String prompt
        atualizar()
    }
    class Turma {
        String nome
        String codigo
        String periodoLetivo
        StatusTurma status
        atualizar()
        encerrar()
        adicionarEstudante()
        estaAtiva()
    }
    class TurmaEstudante {
        Instant associadoEm
    }
    class StatusProblema {
        <<enumeration>>
        ATIVO
        INATIVO
    }
    class StatusTurma {
        <<enumeration>>
        ATIVA
        ENCERRADA
    }
    class TipoEstrategia {
        <<enumeration>>
        CODE_SMELLS
        ACOPLAMENTO_COESAO_PRINCIPIOS
        PADROES_DE_PROJETO
    }

    Professor "1" --> "0..*" Turma : responsavel
    Turma "1" --> "0..*" TurmaEstudante : possui
    Estudante "1" --> "0..*" TurmaEstudante : participa
    Problema --> StatusProblema
    Turma --> StatusTurma
    Estrategia --> TipoEstrategia
```

Problema nasce `ATIVO` e passa a `INATIVO` so por `desativar()`; turma nasce `ATIVA`
e passa a `ENCERRADA` so por `encerrar()`; estudante-turma exige turma ativa e sem duplicata.
