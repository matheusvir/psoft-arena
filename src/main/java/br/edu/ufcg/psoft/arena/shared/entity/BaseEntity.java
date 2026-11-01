package br.edu.ufcg.psoft.arena.shared.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

/**
 * Base das entidades JPA: identidade e igualdade por id.
 *
 * <p>Entidades filhas usam apenas {@code @Getter} (sem {@code @Data}/{@code @EqualsAndHashCode}):
 * campos mutaveis nao participam da igualdade e proxies nao quebram comparacoes. Um id nulo
 * significa entidade ainda nao persistida e so e igual a si mesma (comparacao por identidade).
 */
@Getter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (outro == null || getClass() != outro.getClass()) {
            return false;
        }
        BaseEntity that = (BaseEntity) outro;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
