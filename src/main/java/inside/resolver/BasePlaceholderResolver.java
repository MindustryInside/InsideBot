package inside.resolver;

import inside.service.MessageService;
import inside.util.Lazy;
import org.springframework.util.PropertyPlaceholderHelper;
import reactor.util.context.ContextView;

import java.util.*;
import java.util.function.Supplier;

public abstract class BasePlaceholderResolver implements PropertyPlaceholderHelper.PlaceholderResolver{

    private final Lazy<Map<String, Supplier<?>>> accessors;
    private final ContextView context;
    private final MessageService messageService;

    protected BasePlaceholderResolver(ContextView context, MessageService messageService){
        this.context = Objects.requireNonNull(context, "context");
        this.messageService = Objects.requireNonNull(messageService, "messageService");
        this.accessors = Lazy.of(this::createAccessors);
    }

    protected abstract Map<String, Supplier<?>> createAccessors();

    public Map<String, Supplier<?>> getAccessors(){
        return accessors.get();
    }

    @Override
    public String resolvePlaceholder(String placeholderName){
        Supplier<?> accessor = getAccessors().get(placeholderName);
        if(accessor == null){
            return messageService.get(context, "message.placeholder");
        }
        return String.valueOf(accessor.get());
    }
}
