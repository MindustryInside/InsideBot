package inside.interaction;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.*;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.*;
import discord4j.discordjson.json.*;
import discord4j.rest.util.ApplicationCommandOptionType;
import inside.Settings;
import inside.event.audit.*;
import inside.interaction.model.InteractionDiscordCommand;
import inside.util.*;
import org.joda.time.format.*;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.function.BiConsumer;

import static inside.event.audit.Attribute.COUNT;
import static inside.event.audit.BaseAuditProvider.MESSAGE_TXT;
import static inside.util.ContextUtil.*;

public class InteractionCommands{

    private InteractionCommands(){}

    public static abstract class GuildCommand extends InteractionCommand{
        @Autowired
        protected Settings settings;

        @Override
        public Mono<Boolean> apply(InteractionCommandEnvironment env){
            if(env.event().getInteraction().getMember().isEmpty()){
                return env.event().reply(spec -> spec.addEmbed(embed -> embed.setColor(settings.getDefaults().getErrorColor())
                        .setTitle(messageService.get(env.context(), "message.error.general.title"))
                        .setDescription(messageService.get(env.context(), "command.interaction.only-guild"))))
                        .then(Mono.empty());
            }
            return Mono.just(true);
        }
    }

    @InteractionDiscordCommand
    public static class DeleteCommand extends GuildCommand{
        @Autowired
        private AuditService auditService;

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){
            Member author = env.event().getInteraction().getMember()
                    .orElseThrow(IllegalStateException::new);

            Mono<TextChannel> reply = env.getReplyChannel().cast(TextChannel.class);

            long number = env.event().getInteraction().getCommandInteraction()
                    .getOption("count")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asLong)
                    .orElse(0L);

            if(number <= 0){
                return env.event().reply(spec -> spec.addEmbed(embed -> embed.setColor(settings.getDefaults().getErrorColor())
                        .setTitle(messageService.get(env.context(), "message.error.general.title"))
                        .setDescription(messageService.get(env.context(), "command.incorrect-number"))));
            }else if(number > settings.getDiscord().getMaxClearedCount()){
                return env.event().reply(spec -> spec.addEmbed(embed -> embed.setColor(settings.getDefaults().getErrorColor())
                        .setTitle(messageService.get(env.context(), "message.error.general.title"))
                        .setDescription(messageService.format(env.context(), "common.limit-number",
                                settings.getDiscord().getMaxClearedCount()))));
            }

            StringBuffer result = new StringBuffer();
            Instant limit = Instant.now().minus(14, ChronoUnit.DAYS);
            DateTimeFormatter formatter = DateTimeFormat.forPattern("MM-dd-yyyy HH:mm:ss")
                    .withLocale(env.context().get(KEY_LOCALE))
                    .withZone(env.context().get(KEY_TIMEZONE));

            ReusableByteInputStream input = new ReusableByteInputStream();
            BiConsumer<Message, Member> appendInfo = (message, member) -> {
                result.append("[").append(formatter.print(message.getTimestamp().toEpochMilli())).append("] ");
                if(DiscordUtil.isBot(member)){
                    result.append("[BOT] ");
                }

                result.append(member.getUsername());
                member.getNickname().ifPresent(nickname -> result.append(" (").append(nickname).append(")"));
                result.append(" >");
                String content = MessageUtil.effectiveContent(message);
                if(!content.isBlank()){
                    result.append(" ").append(content);
                }
                if(!message.getEmbeds().isEmpty()){
                    result.append(" (... ").append(message.getEmbeds().size()).append(" embed(s))");
                }
                result.append("\n");
            };

            Mono<Void> history = reply.flatMapMany(channel -> channel.getLastMessage()
                    .flatMapMany(message -> channel.getMessagesBefore(message.getId()))
                    .limitRequest(number)
                    .sort(Comparator.comparing(Message::getId))
                    .filter(message -> message.getTimestamp().isAfter(limit))
                    .flatMap(message -> message.getAuthorAsMember()
                            .doOnNext(member -> {
                                appendInfo.accept(message, member);
                                messageService.deleteById(message.getId());
                            })
                            .thenReturn(message))
                    .transform(messages -> number != 1 ? channel.bulkDeleteMessages(messages).then() : messages.next().flatMap(Message::delete).then()))
                    .then();

            Mono<Void> log = reply.flatMap(channel -> auditService.log(author.getGuildId(), AuditActionType.MESSAGE_CLEAR)
                    .withUser(author)
                    .withChannel(channel)
                    .withAttribute(COUNT, number)
                    .withAttachment(MESSAGE_TXT, input.writeString(result.toString()))
                    .save());

            return history.then(log).and(env.event().reply("✅"));
        }

        @Override
        public ApplicationCommandRequest getRequest(){
            return ApplicationCommandRequest.builder()
                    .name("delete")
                    .description("Delete some messages.")
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("count")
                            .description("How many messages to delete")
                            .type(ApplicationCommandOptionType.INTEGER.getValue())
                            .required(true)
                            .build())
                    .build();
        }
    }

    @InteractionDiscordCommand
    public static class AvatarCommand extends InteractionCommand{
        @Autowired
        private Settings settings;

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){
            return env.event().getInteraction().getCommandInteraction()
                    .getOption("target")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asUser)
                    .orElse(Mono.just(env.event().getInteraction().getUser()))
                    .flatMap(avatar -> env.event().reply(spec -> spec.addEmbed(embed -> embed.setImage(avatar.getAvatarUrl() + "?size=512")
                            .setColor(settings.getDefaults().getNormalColor())
                            .setDescription(messageService.format(env.context(), "command.avatar.text", avatar.getUsername())))));
        }

        @Override
        public ApplicationCommandRequest getRequest(){
            return ApplicationCommandRequest.builder()
                    .name("avatar")
                    .description("Get user avatar.")
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("target")
                            .description("Whose avatar needs to get. By default, your avatar")
                            .type(ApplicationCommandOptionType.USER.getValue())
                            .required(false)
                            .build())
                    .build();
        }
    }

    @InteractionDiscordCommand
    public static class PingCommand extends InteractionCommand{
        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){
            Mono<MessageChannel> reply = env.getReplyChannel();

            long start = System.currentTimeMillis();
            return env.event().acknowledge().then(env.event().getInteractionResponse()
                    .createFollowupMessage(messageService.get(env.context(), "command.ping.testing"))
                    .flatMap(data -> reply.flatMap(channel -> channel.getMessageById(Snowflake.of(data.id())))
                            .flatMap(message -> message.edit(spec -> spec.setContent(messageService.format(env.context(), "command.ping.completed",
                                    System.currentTimeMillis() - start))))))
                    .then();
        }
        @Override
        public ApplicationCommandRequest getRequest(){
            return ApplicationCommandRequest.builder()
                    .name("ping")
                    .description("Get bot ping.")
                    .build();
        }

    }
}
