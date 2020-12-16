package inside.event;

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

import java.util.*;
import java.util.function.Consumer;

import static inside.event.audit.AuditEventType.*;

@Component
public class MessageEventHandler extends AuditEventHandler{
    private static final Logger log = LoggerFactory.getLogger(MessageEventHandler.class);

    @Autowired
    private MemberService memberService;

    @Autowired
    private UserService userService;

    @Autowired
    private CommandHandler commandHandler;

    @Autowired
    private GuildService guildService;

    @Autowired
    private AdminService adminService;

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
        LocalMember localMember = memberService.getOr(member, () -> new LocalMember(member));
        if(localMember.user() == null){
            LocalUser localUser = userService.getOr(userId, () -> new LocalUser(user));
            localMember.user(localUser);
        }

        localMember.addToSeq();
        localMember.lastSentMessage(Calendar.getInstance());
        memberService.save(localMember);

        if(!guildService.exists(guildId)){
            Region region = event.getGuild().flatMap(Guild::getRegion).block();
            GuildConfig guildConfig = new GuildConfig(guildId);
            guildConfig.locale(LocaleUtil.get(region));
            guildConfig.prefix(settings.prefix);
            guildConfig.timeZone(TimeZone.getTimeZone("Etc/Greenwich"));
            guildService.save(guildConfig);
        }

        context.init(guildId);

        if(!MessageUtil.isEmpty(message) && !message.isTts() && message.getEmbeds().isEmpty()){
            MessageInfo info = new MessageInfo();
            info.userId(userId);
            info.messageId(message.getId());
            info.guildId(guildId);
            info.timestamp(Calendar.getInstance());
            info.content(MessageUtil.effectiveContent(message));
            messageService.save(info);
        }

        CommandReference reference = CommandReference
                .builder()
                .event(event)
                .localMember(localMember)
                .channel(() -> channel)
                .build();

        if(adminService.isAdmin(member)){
            return commandHandler.handleMessage(text, reference);
        }
        return Mono.empty();
    }

    @Override
    public Publisher<?> onMessageUpdate(MessageUpdateEvent event){
        Message message = event.getMessage().block();
        if(message == null || message.getChannel().map(Channel::getType).block() != Type.GUILD_TEXT) return Mono.empty();
        User user = message.getAuthor().orElse(null);
        TextChannel c = message.getChannel().cast(TextChannel.class).block();
        if(DiscordUtil.isBot(user) || c == null || !messageService.exists(message.getId())) return Mono.empty();

        MessageInfo info = messageService.getById(event.getMessageId());

        String oldContent = info.content();
        String newContent = MessageUtil.effectiveContent(message);
        boolean under = newContent.length() >= Field.MAX_VALUE_LENGTH || oldContent.length() >= Field.MAX_VALUE_LENGTH;

        if(message.isPinned() || newContent.equals(oldContent)) return Mono.empty();

        context.init(info.guildId());

        Consumer<EmbedCreateSpec> e = embed -> {
            embed.setColor(messageEdit.color);
            embed.setAuthor(user.getUsername(), null, user.getAvatarUrl());
            embed.setTitle(messageService.format("audit.message.edit.title", c.getName()));
            embed.setDescription(messageService.format("audit.message.edit.description",
                                                       c.getGuildId().asString(),
                                                       c.getId().asString(),
                                                       message.getId().asString()));

            embed.addField(messageService.get("audit.message.old-content.title"),
                           MessageUtil.substringTo(oldContent, Field.MAX_VALUE_LENGTH), false);
            embed.addField(messageService.get("audit.message.new-content.title"),
                           MessageUtil.substringTo(newContent, Field.MAX_VALUE_LENGTH), true);

            embed.setFooter(timestamp(), null);
        };

        if(under){
            stringInputStream.writeString(String.format("%s:%n%s%n%n%s:%n%s",
                    messageService.get("audit.message.old-content.title"), oldContent,
                    messageService.get("audit.message.new-content.title"), newContent)
            );
        }

        return log(c.getGuildId(), e, under).then(Mono.fromRunnable(() -> {
            info.content(newContent);
            messageService.save(info);
        }));
    }

    @Override
    public Publisher<?> onMessageDelete(MessageDeleteEvent event){
        Message message = event.getMessage().orElse(null);
        if(message == null || event.getChannel().map(Channel::getType).map(t -> t != Type.GUILD_TEXT).blockOptional().orElse(true)) return Mono.empty();
        Guild guild = message.getGuild().block();
        User user = message.getAuthor().orElse(null);
        TextChannel channel =  event.getChannel().cast(TextChannel.class).block();
        if(guild == null || channel == null || user == null) return Mono.empty();
        if(Objects.equals(channel.getId(), guildService.logChannelId(guild.getId())) && !message.getEmbeds().isEmpty()){ /* =) */
            return guild.getAuditLog(a -> a.setActionType(ActionType.MESSAGE_DELETE)).next().doOnNext(a -> {
                log.warn("Member '{}' deleted log message in guild '{}'",
                         guild.getMemberById(a.getResponsibleUserId()).map(Member::getUsername).block(),
                         guild.getName());
            }).then();
        }
        if(!messageService.exists(message.getId()) || messageService.isCleared(message.getId())) return Mono.empty();

        MessageInfo info = messageService.getById(message.getId());
        String content = info.content();
        boolean under = content.length() >= Field.MAX_VALUE_LENGTH;

        context.init(guild.getId());

        Consumer<EmbedCreateSpec> e = embed -> {
            embed.setColor(messageDelete.color);
            embed.setAuthor(user.getUsername(), null, user.getAvatarUrl());
            embed.setTitle(messageService.format("audit.message.delete.title", channel.getName()));
            embed.setFooter(timestamp(), null);
            embed.addField(messageService.get("audit.message.deleted-content.title"), MessageUtil.substringTo(content, Field.MAX_VALUE_LENGTH), true);
        };

        if(under){
            stringInputStream.writeString(String.format("%s:%n%s", messageService.get("audit.message.deleted-content.title"), content));
        }

        return log(guild.getId(), e, under).then(Mono.fromRunnable(() -> messageService.delete(info)));
    }
}
