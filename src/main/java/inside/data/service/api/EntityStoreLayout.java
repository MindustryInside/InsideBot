package inside.data.service.api;

public interface EntityStoreLayout{

    EntityAccessor getEntityAccessor();

    EntityUpdater getEntityUpdater();
}
