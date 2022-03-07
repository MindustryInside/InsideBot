package inside.data;

import inside.data.api.EntityOperations;
import inside.data.api.r2dbc.DatabaseClient;

import java.util.Objects;

public class DatabaseResources {
    private final DatabaseClient databaseClient;
    private final EntityOperations entityOperations;

    public DatabaseResources(DatabaseClient databaseClient, EntityOperations entityOperations) {
        this.databaseClient = Objects.requireNonNull(databaseClient, "databaseClient");
        this.entityOperations = Objects.requireNonNull(entityOperations, "entityOperations");
    }

    public DatabaseClient getDatabaseClient() {
        return databaseClient;
    }

    public EntityOperations getEntityOperations() {
        return entityOperations;
    }
}
