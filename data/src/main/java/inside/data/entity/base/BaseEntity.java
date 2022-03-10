package inside.data.entity.base;

import inside.data.annotation.Generated;
import inside.data.annotation.Id;
import inside.data.annotation.MapperSuperclass;
import org.immutables.value.Value;

import java.util.Comparator;

import static inside.util.InternalId.getSeqId;

@MapperSuperclass
public interface BaseEntity {

    Comparator<BaseEntity> timestampComparator = Comparator.<BaseEntity>comparingLong(c -> c.id() >>> 10)
            .thenComparingInt(c -> getSeqId(c.id()));

    @Id
    @Generated
    @Value.Default
    default long id() {
        return -1L;
    }
}
