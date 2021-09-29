package inside.command.common;

import discord4j.common.util.Snowflake;
import discord4j.core.object.component.*;
import discord4j.core.object.entity.*;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.core.spec.*;
import discord4j.discordjson.json.gateway.RequestGuildMembers;
import inside.Settings;
import inside.command.Command;
import inside.command.model.*;
import inside.data.entity.GuildConfig;
import inside.interaction.component.selectmenu.SearchSelectMenuListener;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@DiscordCommand(key = "avatar", params = "command.avatar.params", description = "command.avatar.description")
public class AvatarCommand extends Command{

    @Autowired
    private SearchSelectMenuListener searchSelectMenuListener;

    @Autowired
    private Settings settings;

    @Override
    public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
        Member author = env.member();
        Snowflake guildId = author.getGuildId();

        Optional<OptionValue> firstOpt = interaction.getOption(0)
                .flatMap(CommandOption::getValue);

        Mono<User> referencedUser = Mono.justOrEmpty(env.message().getMessageReference())
                .flatMap(ref -> Mono.justOrEmpty(ref.getMessageId()).flatMap(messageId ->
                        env.message().getClient().withRetrievalStrategy(EntityRetrievalStrategy.REST)
                                .getMessageById(ref.getChannelId(), messageId)))
                .flatMap(message -> Mono.justOrEmpty(message.getAuthor()));

        Mono<User> searchUser = Mono.justOrEmpty(firstOpt)
                .map(OptionValue::asString)
                .flatMapMany(s -> env.message()
                        .getClient().requestMembers(RequestGuildMembers.builder()
                                .limit(3)
                                .query(s)
                                .guildId(guildId.asString())
                                .build()))
                .switchIfEmpty(messageService.err(env, "command.incorrect-name").then(Mono.never()))
                .collectList()
                .flatMap(list -> list.size() == 1 ? Mono.just(list.get(0)) : env.message().getChannel()
                        .flatMap(channel -> channel.createMessage(MessageCreateSpec.builder()
                                .content(messageService.get(env.context(), "message.choose-user"))
                                .addComponent(ActionRow.of(SelectMenu.of(
                                        "inside-search-" + author.getId().asString(), list.stream()
                                                .map(member -> SelectMenu.Option.of(member.getNickname()
                                                        .map(s -> String.format("%s (%s)",
                                                                s, member.getTag()))
                                                        .orElseGet(member::getUsername), member.getId().asString()))
                                                .collect(Collectors.toList()))))
                                .build()))
                        .flatMap(message -> searchSelectMenuListener.registerInteraction(
                                message.getId(), e -> e.getClient().getMemberById(
                                        guildId, Snowflake.of(e.event().getValues().get(0)))
                                        .flatMap(target -> e.event().edit(InteractionApplicationCommandCallbackSpec.builder()
                                                .components(List.of())
                                                .content("")
                                                .addEmbed(EmbedCreateSpec.builder()
                                                        .color(settings.getDefaults().getNormalColor())
                                                        .image(target.getAvatarUrl() + "?size=512")
                                                        .description(messageService.format(e.context(), "command.avatar.text", target.getUsername(),
                                                                target.getMention()))
                                                        .build())
                                                .build()))
                                        .then(searchSelectMenuListener.unregisterInteraction(
                                                e.event().getMessageId()))))
                        .then(Mono.never()));

        return Mono.justOrEmpty(firstOpt.map(OptionValue::asSnowflake)).flatMap(id -> env.message().getClient()
                        .withRetrievalStrategy(EntityRetrievalStrategy.REST).getUserById(id))
                .switchIfEmpty(referencedUser)
                .switchIfEmpty(env.message().getClient().withRetrievalStrategy(EntityRetrievalStrategy.REST)
                        .getUserById(env.member().getId())
                        .filter(ignored -> firstOpt.isEmpty()))
                .switchIfEmpty(searchUser)
                .switchIfEmpty(messageService.err(env, "command.incorrect-name").then(Mono.never()))
                .flatMap(target -> env.channel().createMessage(EmbedCreateSpec.builder()
                        .image(target.getAvatarUrl() + "?size=512")
                        .color(settings.getDefaults().getNormalColor())
                        .description(messageService.format(env.context(), "command.avatar.text", target.getUsername(),
                                target.getMention()))
                        .build()))
                .then();
    }

    @Override
    public Mono<Void> help(CommandEnvironment env, String prefix){
        return messageService.infoTitled(env, "command.help.title", "command.avatar.help",
                GuildConfig.formatPrefix(prefix))
                .then();
    }
}
