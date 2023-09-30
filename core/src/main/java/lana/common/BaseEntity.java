package lana.common;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import java.io.Serializable;
import java.util.Objects;

@MappedSuperclass
public abstract class BaseEntity<ID extends Serializable> {
    @Id
    protected ID id;

    public abstract ID getId();

    public abstract void setId(ID id);

    @Override
    public final int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public final boolean equals(Object obj) {
        if (!(obj instanceof BaseEntity<?> base)) {
            return false;
        }

        return Objects.equals(base.id, id);
    }
}
