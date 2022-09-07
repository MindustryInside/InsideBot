package inside.interaction.chatinput;

import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.channel.Channel;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.possible.Possible;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.PermissionCategory;
import inside.interaction.annotation.*;
import inside.service.MessageService;
import inside.util.Preconditions;
import inside.util.ResourceMessageSource;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

public abstract class InteractionCommand {

    protected final Info info = Info.from(getClass());
    protected final MessageService messageService;

    protected InteractionCommand(MessageService messageService) {
        this.messageService = messageService;
    }

    public Publisher<?> execute(ChatInputInteractionEnvironment env) {
        return Mono.empty();
    }

    public Mono<Boolean> filter(ChatInputInteractionEnvironment env) {
        return Mono.just(true);
    }

    public ApplicationCommandRequest asRequest() {
        return ApplicationCommandRequest.builder()
                .name(messageService.get(info.nameCode + ".name"))
                .nameLocalizationsOrNull(getAll(info.nameCode + ".name"))
                .description(messageService.get(info.nameCode + ".description"))
                .descriptionLocalizationsOrNull(getAll(info.nameCode + ".description"))
                .options(info.options.stream()
                        .map(this::fromOption)
                        .collect(Collectors.toList()))
                .build();
    }

    protected Possible<Double> unmapAbsentDouble(double val) {
        return val != -1 ? Possible.of(val) : Possible.absent();
    }

    protected Possible<Integer> unmapAbsentInt(int val) {
        return val != -1 ? Possible.of(val) : Possible.absent();
    }

    protected ApplicationCommandOptionData fromOption(Option ann) {
        Preconditions.requireArgument(ann.type() == ApplicationCommandOption.Type.STRING ||
                ann.minLength() == -1 && ann.maxLength() == -1, () ->
                "Option " + ann.name() + " in command: " + info.nameCode +
                        " have a length restriction, but it's type is: " + ann.type());

        Preconditions.requireArgument(ann.type() == ApplicationCommandOption.Type.NUMBER ||
                        ann.type() == ApplicationCommandOption.Type.INTEGER || ann.minValue() == -1 && ann.maxValue() == -1,
                "Option " + ann.name() + " in command: " + info.nameCode
                        + " have a value restriction, but it's type is: " + ann.type());

        String nameCode = info.nameCode + ".options." + ann.name();

        return ApplicationCommandOptionData.builder()
                .name(messageService.get(nameCode + ".name"))
                .nameLocalizationsOrNull(getAll(nameCode + ".name"))
                .description(messageService.get(nameCode + ".description"))
                .descriptionLocalizationsOrNull(getAll(nameCode + ".description"))
                .type(ann.type().getValue())
                .required(ann.required())
                .choices(Arrays.stream(ann.choices())
                        .map(c -> fromChoice(c, ann.type()))
                        .collect(Collectors.toList()))
                .autocomplete(ann.autocomplete())
                .channelTypes(Arrays.stream(ann.channelTypes())
                        .map(Channel.Type::getValue)
                        .collect(Collectors.toList()))
                .minValue(unmapAbsentDouble(ann.minValue()))
                .maxValue(unmapAbsentDouble(ann.maxValue()))
                .minLength(unmapAbsentInt(ann.minLength()))
                .maxLength(unmapAbsentInt(ann.maxLength()))
                .build();
    }

    private ApplicationCommandOptionChoiceData fromChoice(Choice choice, ApplicationCommandOption.Type type) {
        // не хочется верифицировать :/
        Object o = switch (type) {
            case STRING, INTEGER, NUMBER -> choice.value();
            default -> throw new IllegalArgumentException("Unexpected choice value for type: " + type);
        };

        // TODO возможно не лучший формат для имени
        String nameCode = info.nameCode + ".choices." + choice.name();
        return ApplicationCommandOptionChoiceData.builder()
                .name(messageService.get(nameCode))
                .nameLocalizationsOrNull(getAll(nameCode))
                .value(o)
                .build();
    }

    protected Map<String, String> getAll(String code) {
        return ResourceMessageSource.supportedLocaled.stream()
                .filter(l -> !l.equals(messageService.configuration.discord().locale()) &&
                        messageService.messageSource.contains(code, l))
                .collect(Collectors.toMap(Locale::toString, l -> messageService.messageSource.get(code, l)));
    }

    protected record Info(String nameCode, ApplicationCommandOption.Type type,
                          Set<PermissionCategory> permissions,
                          List<Option> options) {

        private static Info from(Class<? extends InteractionCommand> type) {
            var chatInputCommand = type.getDeclaredAnnotation(ChatInputCommand.class);
            if (chatInputCommand != null) {
                return new Info(chatInputCommand.value(),
                        ApplicationCommandOption.Type.UNKNOWN,
                        Collections.unmodifiableSet(EnumSet.copyOf(Arrays.asList(chatInputCommand.permissions()))),
                        List.of(type.getDeclaredAnnotationsByType(Option.class)));
            }

            var subcommand = type.getDeclaredAnnotation(Subcommand.class);
            if (subcommand != null) {
                return new Info(subcommand.value(), ApplicationCommandOption.Type.SUB_COMMAND,
                        Set.of(), List.of(type.getDeclaredAnnotationsByType(Option.class)));
            }

            var subcommandGroup = type.getDeclaredAnnotation(SubcommandGroup.class);
            if (subcommandGroup != null) {
                return new Info(subcommandGroup.value(), ApplicationCommandOption.Type.SUB_COMMAND_GROUP, Set.of(), List.of());
            }

            throw new IllegalArgumentException("Missed annotation in class: " + type);
        }
    }
}
