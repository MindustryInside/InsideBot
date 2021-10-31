package inside.data.service.impl;

import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.AllowedMentions;
import inside.Settings;
import inside.data.entity.WelcomeMessage;
import inside.data.repository.WelcomeMessageRepository;
import inside.data.service.BaseLongObjEntityService;
import inside.resolver.*;
import inside.service.MessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.PropertyPlaceholderHelper;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.util.Locale;

@Service
public class WelcomeMessageService extends BaseLongObjEntityService<WelcomeMessage, WelcomeMessageRepository>{

    private static final PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper("${", "}");

    private final MessageService messageService;

    protected WelcomeMessageService(WelcomeMessageRepository repository, Settings settings, MessageService messageService){
        super(repository, settings.getCache().isWelcomeMessage());
        this.messageService = messageService;
    }

    @Nullable
    @Override
    @Transactional(readOnly = true)
    protected WelcomeMessage find0(long id){
        return repository.findByGuildId(id);
    }

    @Override
    protected Object extractId(WelcomeMessage entity){
        return entity.getGuildId().asLong();
    }

    public Mono<MessageCreateSpec> compile(MessageTemplate messageTemplate){
        return Mono.deferContextual(ctx -> {
            var messageSpec = MessageCreateSpec.builder();

            String template = messageTemplate.template().getMessage();
            String resolved = helper.replacePlaceholders(template,
                    MemberPlaceholderResolver.of(ctx, messageService, messageTemplate.member()));
            if(!resolved.isBlank()){
                messageSpec.content(resolved);
            }

            if(template.toLowerCase(Locale.ROOT).contains("mention")){
                messageSpec.allowedMentions(AllowedMentions.builder()
                        .allowUser(messageTemplate.member().getId())
                        .build());
            }

            return Mono.just(messageSpec.build());
        });
    }
}
