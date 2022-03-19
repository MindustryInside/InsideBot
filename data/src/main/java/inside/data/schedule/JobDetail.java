package inside.data.schedule;

import inside.util.Preconditions;
import org.immutables.value.Value;

import java.lang.reflect.Modifier;
import java.util.Map;

@Value.Immutable
public abstract class JobDetail {

    public static ImmutableJobDetail.Builder builder() {
        return ImmutableJobDetail.builder();
    }

    public abstract Key key();

    public abstract Class<? extends Job> jobClass();

    @Value.Default
    public Map<String, Object> jobData() {
        return Map.of();
    }

    @Value.Check
    protected void validate() {
        Preconditions.requireState(jobClass() != Job.class, () ->
                "'jobClass' must be not " + Job.class.getCanonicalName());
        Preconditions.requireState(!Modifier.isAbstract(jobClass().getModifiers()), "'jobClass' must be not interface or abstract class");
    }
}
