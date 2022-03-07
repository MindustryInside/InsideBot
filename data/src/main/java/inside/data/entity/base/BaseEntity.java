package inside.data.entity.base;

import inside.data.annotation.Generated;
import inside.data.annotation.Id;
import inside.data.annotation.MapperSuperclass;
import org.immutables.value.Value;

@MapperSuperclass
public interface BaseEntity {

    @Id
    @Generated
    @Value.Default
    default long id() {
        return -1L;
    }
}
