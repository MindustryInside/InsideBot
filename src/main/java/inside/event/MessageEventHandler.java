package inside.event;

import arc.files.*;
import arc.util.Strings;
import arc.util.io.*;
import arc.util.serialization.Base64Coder;
import com.fasterxml.jackson.databind.ser.Serializers;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.*;
import discord4j.core.object.Embed.Field;
import discord4j.core.object.Region;
import discord4j.core.object.audit.ActionType;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.*;
import discord4j.core.object.entity.channel.Channel.Type;
import discord4j.core.spec.*;
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
import reactor.core.publisher.*;
import reactor.util.context.Context;
import reactor.util.function.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.*;

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
            info.content(message.getContent());
            Map<String, String> attachments = Flux.fromIterable(message.getAttachments())
                    .flatMap(attachment ->
                            Flux.using(() -> new URL(attachment.getUrl()).openConnection().getInputStream(),
                                       input -> Mono.fromCallable(() -> {
                                           try{
                                               return Tuples.of(attachment.getFilename(), Base64Coder.encodeLines(input.readAllBytes()));
                                           }catch(IOException e){
                                               log.warn("Failed to download file '{}'; skipping...", attachment.getFilename());
                                               return null;
                                           }
                                       }), Streams::close))
                    .collect(Collectors.toMap(Tuple2::getT1, Tuple2::getT2))
                    .block();

            info.attachments(attachments);
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
        String newContent = message.getContent();
        boolean under = newContent.length() >= Field.MAX_VALUE_LENGTH || oldContent.length() >= Field.MAX_VALUE_LENGTH;

        if(message.isPinned() || newContent.equals(oldContent)){
            return Mono.empty();
        }

        Snowflake guildId = info.guildId();
        context = Context.of(KEY_GUILD_ID, guildId,
                             KEY_LOCALE, discordEntityRetrieveService.locale(guildId),
                             KEY_TIMEZONE, discordEntityRetrieveService.timeZone(guildId));

        Consumer<EmbedCreateSpec> embed = spec -> {
            spec.setColor(messageEdit.color);
            spec.setAuthor(user.getUsername(), null, user.getAvatarUrl());
            spec.setTitle(messageService.format(context, "audit.message.edit.title", c.getName()));
            spec.setDescription(messageService.format(context, "audit.message.edit.description",
                                                      c.getGuildId().asString(),
                                                      c.getId().asString(),
                                                      message.getId().asString()));

            spec.addField(messageService.get(context, "audit.message.old-content.title"),
                           MessageUtil.substringTo(oldContent, Field.MAX_VALUE_LENGTH), false);
            spec.addField(messageService.get(context, "audit.message.new-content.title"),
                           MessageUtil.substringTo(newContent, Field.MAX_VALUE_LENGTH), true);

            spec.setFooter(timestamp(), null);
        };

        MessageCreateSpec spec = new MessageCreateSpec().setEmbed(embed);
        if(under){
            stringInputStream.writeString(String.format("%s:%n%s%n%n%s:%n%s",
                    messageService.get(context, "audit.message.old-content.title"), oldContent,
                    messageService.get(context, "audit.message.new-content.title"), newContent)
            );
            spec.addFile("message.txt", stringInputStream);
        }

        //todo
        if(info.attachments().size() > 1){
            try(ByteArrayOutputStream zip = new ByteArrayOutputStream();
                ZipOutputStream out = new ZipOutputStream(zip)){

                info.attachments().forEach((key, value) -> {
                    try(ByteArrayOutputStream o = new ReusableByteOutStream()){
                        o.writeBytes(Base64Coder.decodeLines(value));
                        ZipEntry entry = new ZipEntry(key);
                        entry.setSize(o.size());
                        out.putNextEntry(entry);
                        Streams.copy(new ByteArrayInputStream(o.toByteArray()), out);
                        out.closeEntry();
                    }catch(IOException e){
                        throw new RuntimeException(e);
                    }
                });

                out.finish();
                spec.addFile("attachments.zip", new ByteArrayInputStream(zip.toByteArray()));
            }catch(IOException e){
                throw new RuntimeException(e);
            }
        }else if(info.attachments().size() == 1){
            //todo this is terrible...
            info.attachments().forEach((key, value) -> {
                stringInputStream.setBytes(Base64Coder.decodeLines(value));
                spec.addFile(key, stringInputStream);
            });
        }

        return log(c.getGuildId(), spec).contextWrite(context).then(Mono.fromRunnable(() -> {
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

        Consumer<EmbedCreateSpec> embed = spec -> {
            spec.setColor(messageDelete.color);
            spec.setAuthor(user.getUsername(), null, user.getAvatarUrl());
            spec.setTitle(messageService.format(context, "audit.message.delete.title", channel.getName()));
            spec.setFooter(timestamp(), null);
            spec.addField(messageService.get(context, "audit.message.deleted-content.title"),
                           MessageUtil.substringTo(content, Field.MAX_VALUE_LENGTH), true);
        };

        MessageCreateSpec spec = new MessageCreateSpec().setEmbed(embed);
        if(under){
            stringInputStream.writeString(String.format("%s:%n%s", messageService.get(context, "audit.message.deleted-content.title"), content));
            spec.addFile("message.txt", stringInputStream);
        }

        if(info.attachments().size() > 1){
            try(ByteArrayOutputStream zip = new ByteArrayOutputStream();
                ZipOutputStream out = new ZipOutputStream(zip)){

                info.attachments().forEach((key, value) -> {
                    try(ByteArrayOutputStream o = new ReusableByteOutStream()){
                        o.writeBytes(Base64Coder.decodeLines(value));
                        ZipEntry entry = new ZipEntry(key);
                        entry.setSize(o.size());
                        out.putNextEntry(entry);
                        Streams.copy(new ByteArrayInputStream(o.toByteArray()), out);
                        out.closeEntry();
                    }catch(IOException e){
                        throw new RuntimeException(e);
                    }
                });

                out.finish();
                spec.addFile("attachments.zip", new ByteArrayInputStream(zip.toByteArray()));
            }catch(IOException e){
                throw new RuntimeException(e);
            }
        }else if(info.attachments().size() == 1){
            //todo this is terrible...
            info.attachments().forEach((key, value) -> {
                stringInputStream.setBytes(Base64Coder.decodeLines(value));
                spec.addFile(key, stringInputStream);
            });
        }

        return log(guild.getId(), spec).contextWrite(context).then(Mono.fromRunnable(() -> messageService.delete(info)));
    }
}
