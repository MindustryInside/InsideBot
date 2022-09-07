package inside.command.common;

import discord4j.core.object.entity.User;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.core.spec.EmbedCreateSpec;
import inside.command.*;
import inside.interaction.util.MemberSearch;
import inside.service.MessageService;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Optional;

@DiscordCommand(key = "commands.avatar.key", params = "commands.avatar.params", description = "commands.avatar.desc")
public class AvatarCommand extends Command {

    public AvatarCommand(MessageService messageService) {
        super(messageService);
    }

    @Override
    public Publisher<?> execute(CommandEnvironment env, CommandInteraction interaction) {

        Optional<OptionValue> firstOpt = interaction.getOption(0)
                .flatMap(CommandOption::getValue);

        Mono<User> referencedUser = Mono.justOrEmpty(env.message().getMessageReference())
                .flatMap(ref -> Mono.justOrEmpty(ref.getMessageId()).flatMap(messageId ->
                        env.message().getClient().withRetrievalStrategy(EntityRetrievalStrategy.REST)
                                .getMessageById(ref.getChannelId(), messageId)))
                .flatMap(message -> Mono.justOrEmpty(message.getAuthor()));

        return Mono.justOrEmpty(firstOpt.map(OptionValue::asSnowflake))
                .flatMap(id -> env.message().getClient()
                        .withRetrievalStrategy(EntityRetrievalStrategy.REST)
                        .getUserById(id))
                .switchIfEmpty(referencedUser)
                .switchIfEmpty(env.message().getClient()
                        .withRetrievalStrategy(EntityRetrievalStrategy.REST)
                        .getUserById(env.member().getId())
                        .filter(ignored -> firstOpt.isEmpty()))
                .switchIfEmpty(Mono.justOrEmpty(firstOpt.map(OptionValue::value))
                        .flatMap(q -> MemberSearch.search(env, q)))
                .switchIfEmpty(messageService.err(env, messageService.get(null,"inside.static.user-by-id-not-found")).then(Mono.never()))
                .flatMap(target -> env.channel().createMessage(EmbedCreateSpec.builder()
                        .image(target.getAvatarUrl() + "?size=512")
                        .color(env.configuration().discord().embedColor())
                        .description(String.format(messageService.get(null,"commands.avatar.message"), target.getUsername(), target.getMention()))
                        .build()))
                .then();
    }
}
