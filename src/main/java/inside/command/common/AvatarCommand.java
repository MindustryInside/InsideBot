package inside.command.common;

import discord4j.core.object.entity.User;
import discord4j.core.retriever.EntityRetrievalStrategy;
import inside.command.Command;
import inside.command.model.*;
import inside.data.entity.GuildConfig;
import reactor.core.publisher.Mono;

import java.util.Optional;

@DiscordCommand(key = "avatar", params = "command.avatar.params", description = "command.avatar.description")
public class AvatarCommand extends Command{
    @Override
    public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
        Optional<OptionValue> firstOpt = interaction.getOption(0)
                .flatMap(CommandOption::getValue);

        Mono<User> referencedUser = Mono.justOrEmpty(env.getMessage().getMessageReference())
                .flatMap(ref -> Mono.justOrEmpty(ref.getMessageId()).flatMap(messageId ->
                        env.getMessage().getClient().withRetrievalStrategy(EntityRetrievalStrategy.REST)
                                .getMessageById(ref.getChannelId(), messageId)))
                .flatMap(message -> Mono.justOrEmpty(message.getAuthor()));

        return Mono.justOrEmpty(firstOpt.map(OptionValue::asSnowflake)).flatMap(id -> env.getMessage().getClient()
                        .withRetrievalStrategy(EntityRetrievalStrategy.REST).getUserById(id))
                .switchIfEmpty(referencedUser)
                .switchIfEmpty(env.getMessage().getClient().withRetrievalStrategy(EntityRetrievalStrategy.REST)
                        .getUserById(env.getAuthorAsMember().getId())
                        .filter(ignored -> firstOpt.isEmpty()))
                .switchIfEmpty(messageService.err(env, "command.incorrect-name").then(Mono.empty()))
                .flatMap(user -> messageService.info(env, embed -> embed.image(user.getAvatarUrl() + "?size=512")
                        .description(messageService.format(env.context(), "command.avatar.text", user.getUsername(),
                                user.getMention()))));
    }

    @Override
    public Mono<Void> help(CommandEnvironment env, String prefix){
        return messageService.infoTitled(env, "command.help.title", "command.avatar.help",
                GuildConfig.formatPrefix(prefix));
    }
}
