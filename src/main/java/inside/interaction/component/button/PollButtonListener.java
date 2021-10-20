package inside.interaction.component.button;

import discord4j.core.object.Embed;
import discord4j.core.object.entity.*;
import discord4j.core.spec.*;
import inside.data.entity.PollAnswer;
import inside.data.service.EntityRetriever;
import inside.interaction.ButtonEnvironment;
import inside.interaction.annotation.ComponentProvider;
import inside.service.MessageService;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@ComponentProvider("inside-poll")
public class PollButtonListener implements ButtonListener{

    private final EntityRetriever entityRetriever;
    private final MessageService messageService;

    public PollButtonListener(@Autowired EntityRetriever entityRetriever,
                              @Autowired MessageService messageService){
        this.entityRetriever = entityRetriever;
        this.messageService = messageService;
    }

    @Override
    public Publisher<?> handle(ButtonEnvironment env){
        return entityRetriever.getPollById(env.event().getMessageId()).flatMap(poll -> {
            User user = env.event().getInteraction().getUser();
            if(poll.getAnswered().stream().anyMatch(p -> p.getUserId().equals(user.getId()))){
                return messageService.err(env, "command.poll.already-answered");
            }

            String[] parts = env.event().getCustomId().split("-");
            int idx = Integer.parseInt(parts[2]); // [ inside, poll, 0 ]

            Message message = env.event().getMessage().orElseThrow();
            List<Embed> embeds = message.getEmbeds();
            Embed source = embeds.isEmpty() ? null : embeds.get(0);
            var embedSpec = EmbedCreateSpec.builder();

            if(source == null){ // embed removed and we can't handle interaction, TODO: implement recreating
                return env.event().getInteractionResponse()
                        .deleteInitialResponse()
                        .and(entityRetriever.delete(poll));
            }else{
                embedSpec.author(source.getAuthor()
                        .map(author -> EmbedCreateFields.Author.of(
                                author.getName().orElseThrow(), null,
                                author.getIconUrl().orElseThrow()))
                        .orElseThrow());
                embedSpec.title(source.getTitle().orElseThrow());
                embedSpec.color(source.getColor().orElseThrow());
                embedSpec.description(source.getDescription().orElseThrow());
            }

            PollAnswer answer = new PollAnswer();
            answer.setGuildId(env.event().getInteraction().getGuildId().orElseThrow());
            answer.setOption(idx);
            answer.setUserId(user.getId());

            poll.getAnswered().add(answer);

            int count = poll.getAnswered().size();
            Map<Integer, Integer> statistic = new LinkedHashMap<>();
            for(PollAnswer pollAnswer : poll.getAnswered()){
                statistic.compute(pollAnswer.getOption(), (s, i) -> i == null ? 1 : i + 1);
            }

            embedSpec.footer(statistic.entrySet().stream()
                    .map(e -> String.format("%s: %d%% (%d)",
                            e.getKey(), (int)(e.getValue() * 100f / count), e.getValue()))
                    .collect(Collectors.joining("\n")), null);

            return env.event().edit(InteractionApplicationCommandCallbackSpec.builder()
                            .addEmbed(embedSpec.build())
                            .build())
                    .then(entityRetriever.save(poll))
                    .then();
        });
    }
}
