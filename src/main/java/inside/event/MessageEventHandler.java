package inside.event;

import discord4j.common.LogUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.*;
import discord4j.core.object.Embed.Field;
import discord4j.core.object.Region;
import discord4j.core.object.audit.ActionType;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.*;
import discord4j.core.object.entity.channel.Channel.Type;
import discord4j.core.spec.EmbedCreateSpec;
import inside.Settings;
import inside.common.command.model.base.CommandReference;
import inside.common.command.service.CommandHandler;
import inside.data.entity.*;
import inside.data.service.*;
import inside.event.audit.AuditEventHandler;
import inside.util.*;
import org.reactivestreams.Publisher;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.context.*;

import java.util.*;
import java.util.function.Consumer;

import static inside.event.audit.AuditEventType.*;
import static inside.util.ContextUtil.*;

@Component
public class MessageEventHandler extends AuditEventHandler{
    private static final Logger log = LoggerFactory.getLogger(MessageEventHandler.class);

    @Autowired
    private AdminService adminService;

    @Autowired
    private CommandHandler commandHandler;

    @Autowired
    private DiscordEntityRetrieveService discordEntityRetrieveService;

    @Autowired
    private Settings settings;

    @Override
    public Publisher<?> onReady(ReadyEvent event){ // не триггерится, баг текущей версии d4j
        return Mono.fromRunnable(() -> log.info("Bot up."));
    }

    @Override
    public Publisher<?> onMessageCreate(MessageCreateEvent event){
        Message message = event.getMessage();
        String text = message.getContent().trim();
        Member member = event.getMember().orElse(null);
        if(member == null || message.getType() != Message.Type.DEFAULT || message.getChannel().map(Channel::getType).block() != Type.GUILD_TEXT) return Mono.empty();
        Mono<TextChannel> channel = message.getChannel().cast(TextChannel.class);
        User user = message.getAuthor().orElse(null);
        Snowflake guildId = event.getGuildId().orElse(null);
        if(DiscordUtil.isBot(user) || guildId == null) return Mono.empty();
        Snowflake userId = user.getId();
        LocalMember localMember = discordEntityRetrieveService.getMember(member, () -> new LocalMember(member));

        localMember.addToSeq();
        localMember.lastSentMessage(Calendar.getInstance());
        discordEntityRetrieveService.save(localMember);

        if(!discordEntityRetrieveService.existsGuildById(guildId)){
            Region region = event.getGuild().flatMap(Guild::getRegion).block();
            GuildConfig guildConfig = new GuildConfig(guildId);
            guildConfig.locale(LocaleUtil.get(region));
            guildConfig.prefix(settings.prefix);
            guildConfig.timeZone(TimeZone.getTimeZone("Etc/Greenwich"));
            discordEntityRetrieveService.save(guildConfig);
        }

        if(!MessageUtil.isEmpty(message) && !message.isTts() && message.getEmbeds().isEmpty()){
            MessageInfo info = new MessageInfo();
            info.userId(userId);
            info.messageId(message.getId());
            info.guildId(guildId);
            info.timestamp(Calendar.getInstance());
            info.content(MessageUtil.effectiveContent(message));
            messageService.save(info);
        }

        context = Context.of(KEY_GUILD_ID, guildId,
                             KEY_LOCALE, discordEntityRetrieveService.locale(guildId),
                             KEY_TIMEZONE, discordEntityRetrieveService.timeZone(guildId));

        CommandReference reference = CommandReference.builder()
                .event(event)
                .context(context)
                .localMember(localMember)
                .channel(() -> channel)
                .build();

        if(adminService.isAdmin(member)){
            return commandHandler.handleMessage(text, reference).contextWrite(context);
        }
        return Mono.empty();
    }

    @Override
    public Publisher<?> onMessageUpdate(MessageUpdateEvent event){
        Message message = event.getMessage().block();
        if(message == null || message.getChannel().map(Channel::getType).block() != Type.GUILD_TEXT){
            return Mono.empty();
        }

        User user = message.getAuthor().orElse(null);
        TextChannel c = message.getChannel().cast(TextChannel.class).block();
        if(DiscordUtil.isBot(user) || c == null || !messageService.exists(message.getId())){
            return Mono.empty();
        }

        MessageInfo info = messageService.getById(event.getMessageId());

        String oldContent = info.content();
        String newContent = MessageUtil.effectiveContent(message);
        boolean under = newContent.length() >= Field.MAX_VALUE_LENGTH || oldContent.length() >= Field.MAX_VALUE_LENGTH;

        if(message.isPinned() || newContent.equals(oldContent)){
            return Mono.empty();
        }

        Snowflake guildId = info.guildId();
        context = Context.of(KEY_GUILD_ID, guildId,
                             KEY_LOCALE, discordEntityRetrieveService.locale(guildId),
                             KEY_TIMEZONE, discordEntityRetrieveService.timeZone(guildId));

        Consumer<EmbedCreateSpec> e = embed -> {
            embed.setColor(messageEdit.color);
            embed.setAuthor(user.getUsername(), null, user.getAvatarUrl());
            embed.setTitle(messageService.format(context, "audit.message.edit.title", c.getName()));
            embed.setDescription(messageService.format(context, "audit.message.edit.description",
                                                       c.getGuildId().asString(),
                                                       c.getId().asString(),
                                                       message.getId().asString()));

            embed.addField(messageService.get(context, "audit.message.old-content.title"),
                           MessageUtil.substringTo(oldContent, Field.MAX_VALUE_LENGTH), false);
            embed.addField(messageService.get(context, "audit.message.new-content.title"),
                           MessageUtil.substringTo(newContent, Field.MAX_VALUE_LENGTH), true);

            embed.setFooter(timestamp(), null);
        };

        if(under){
            stringInputStream.writeString(String.format("%s:%n%s%n%n%s:%n%s",
                    messageService.get(context, "audit.message.old-content.title"), oldContent,
                    messageService.get(context, "audit.message.new-content.title"), newContent)
            );
        }

        return log(c.getGuildId(), e, under).contextWrite(context).then(Mono.fromRunnable(() -> {
            info.content(newContent);
            messageService.save(info);
        }));
    }

    @Override
    public Publisher<?> onMessageDelete(MessageDeleteEvent event){
        Message message = event.getMessage().orElse(null);
        if(message == null || event.getChannel().map(Channel::getType).map(t -> t != Type.GUILD_TEXT).blockOptional().orElse(true)){
            return Mono.empty();
        }

        Guild guild = message.getGuild().block();
        User user = message.getAuthor().orElse(null);
        TextChannel channel =  event.getChannel().cast(TextChannel.class).block();
        if(guild == null || channel == null || user == null){
            return Mono.empty();
        }

        if(Objects.equals(channel.getId(), discordEntityRetrieveService.logChannelId(guild.getId())) && !message.getEmbeds().isEmpty()){ /* =) */
            return guild.getAuditLog(a -> a.setActionType(ActionType.MESSAGE_DELETE)).next().doOnNext(a -> {
                log.warn("Member '{}' deleted log message in guild '{}'",
                         guild.getMemberById(a.getResponsibleUserId()).map(Member::getUsername).block(),
                         guild.getName());
            }).then();
        }
        if(!messageService.exists(message.getId()) || messageService.isCleared(message.getId())){
            return Mono.empty();
        }

        MessageInfo info = messageService.getById(message.getId());
        String content = info.content();
        boolean under = content.length() >= Field.MAX_VALUE_LENGTH;

        context = Context.of(KEY_GUILD_ID, guild.getId(),
                             KEY_LOCALE, discordEntityRetrieveService.locale(guild.getId()),
                             KEY_TIMEZONE, discordEntityRetrieveService.timeZone(guild.getId()));

        Consumer<EmbedCreateSpec> e = embed -> {
            embed.setColor(messageDelete.color);
            embed.setAuthor(user.getUsername(), null, user.getAvatarUrl());
            embed.setTitle(messageService.format(context, "audit.message.delete.title", channel.getName()));
            embed.setFooter(timestamp(), null);
            embed.addField(messageService.get(context, "audit.message.deleted-content.title"),
                           MessageUtil.substringTo(content, Field.MAX_VALUE_LENGTH), true);
        };

        if(under){
            stringInputStream.writeString(String.format("%s:%n%s", messageService.get(context, "audit.message.deleted-content.title"), content));
        }

        return log(guild.getId(), e, under).contextWrite(context).then(Mono.fromRunnable(() -> messageService.delete(info)));
    }
}
