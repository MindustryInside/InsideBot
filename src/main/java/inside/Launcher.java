package inside;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import discord4j.common.JacksonResources;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.object.entity.Guild;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandPermissionsData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.PartialGuildApplicationCommandPermissionsData;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.request.RouteMatcher;
import discord4j.rest.response.ResponseFunction;
import discord4j.rest.route.Routes;
import discord4j.rest.util.AllowedMentions;
import inside.data.CacheEntityRetriever;
import inside.data.DatabaseResources;
import inside.data.EntityRetrieverImpl;
import inside.data.RepositoryHolder;
import inside.data.api.DefaultRepositoryFactory;
import inside.data.api.EntityOperations;
import inside.data.api.codec.LocaleCodec;
import inside.data.api.r2dbc.DefaultDatabaseClient;
import inside.event.InteractionEventHandler;
import inside.event.MessageEventHandler;
import inside.event.ReactionRoleEventHandler;
import inside.event.StarboardEventHandler;
import inside.interaction.chatinput.InteractionCommand;
import inside.interaction.chatinput.InteractionCommandHolder;
import inside.interaction.chatinput.InteractionGuildCommand;
import inside.interaction.chatinput.admin.DeleteCommand;
import inside.interaction.chatinput.common.*;
import inside.interaction.chatinput.settings.ActivityCommand;
import inside.interaction.chatinput.settings.ReactionRolesCommand;
import inside.interaction.chatinput.settings.StarboardCommand;
import inside.service.InteractionService;
import inside.service.MessageService;
import inside.service.task.ActivityTask;
import inside.util.ResourceMessageSource;
import inside.util.func.UnsafeRunnable;
import inside.util.json.AdapterModule;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.R2dbcBadGrammarException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;
import sun.misc.Signal;

import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static reactor.function.TupleUtils.function;

public class Launcher {
    public static final AllowedMentions suppressAll = AllowedMentions.suppressAll();

    private static final Logger log = Loggers.getLogger(Launcher.class);

    private static GatewayDiscordClient gateway;

    public static GatewayDiscordClient getClient() {
        return gateway;
    }

    private static void printBanner() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (var banner = loader.getResourceAsStream("banner.txt")) {
            Objects.requireNonNull(banner, "banner");
            System.out.println(new String(banner.readAllBytes()));
        } catch (Throwable t) {
            log.error("Failed to print banner.", t);
        }
    }

    public static void main(String[] args) {
        long pre = System.currentTimeMillis();
        printBanner();

        JacksonResources jacksonResources = JacksonResources.create()
                .withMapperFunction(objectMapper -> objectMapper
                        .registerModule(new JavaTimeModule())
                        .registerModule(new AdapterModule())
                        .registerModule(new ParameterNamesModule()));

        Configuration configuration;
        File cfg = new File("configuration.json");
        try {
            if (cfg.exists()) {
                configuration = jacksonResources.getObjectMapper().readValue(cfg, Configuration.class);
            } else {
                String json = jacksonResources.getObjectMapper()
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(ImmutableConfiguration.builder()
                                .token("<your token>")
                                .build());

                Files.writeString(cfg.toPath(), json);
                log.info("Bot configuration created.");
                return;
            }
        } catch (Throwable t) {
            log.error("Failed to resolve bot configuration.", t);
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Configuration: {}", configuration);
        }

        Hooks.onNextError((t, o) -> {
            if (t instanceof R2dbcBadGrammarException p) {
                log.error("Failed to execute SQL '{}'", p.getOffendingSql());
            }
            return t;
        });

        String url = configuration.database().url();
        String user = configuration.database().user();
        String password = configuration.database().password();

        var parsed = ConnectionFactoryOptions.parse(url);
        var psqlConfiguration = PostgresqlConnectionConfiguration.builder()
                .codecRegistrar((connection, allocator, registry) -> Mono.fromRunnable(() -> {
                    registry.addLast(new LocaleCodec(allocator));
                }))
                .username(user)
                .password(password)
                // ???! Зачем они убрали дженерик?
                .port((Integer) parsed.getRequiredValue(ConnectionFactoryOptions.PORT))
                .host((String) parsed.getRequiredValue(ConnectionFactoryOptions.HOST))
                .database((String) parsed.getRequiredValue(ConnectionFactoryOptions.DATABASE))
                .build();

        var factory = new PostgresqlConnectionFactory(psqlConfiguration);

        var poolConfiguration = ConnectionPoolConfiguration.builder(factory)
                .maxSize(30)
                .maxLifeTime(Duration.ofSeconds(10))
                .build();
        var pool = new ConnectionPool(poolConfiguration);

        var entityOperations = new EntityOperations(jacksonResources);
        var databaseClient = new DefaultDatabaseClient(pool);
        var databaseResources = new DatabaseResources(databaseClient, entityOperations);

        var repositoryFactory = new DefaultRepositoryFactory(databaseResources);
        var repositoryHolder = new RepositoryHolder(repositoryFactory);

        var entityRetriever = new CacheEntityRetriever(new EntityRetrieverImpl(configuration, repositoryHolder));
        var messageSource = new ResourceMessageSource("bundle");

        DiscordClient.builder(configuration.token())
                .onClientResponse(ResponseFunction.emptyIfNotFound())
                .onClientResponse(ResponseFunction.emptyOnErrorStatus(RouteMatcher.route(Routes.REACTION_CREATE), 400))
                .setDefaultAllowedMentions(suppressAll)
                .build()
                .gateway()
                .setEnabledIntents(IntentSet.of(
                        Intent.GUILDS,
                        Intent.GUILD_MEMBERS,
                        Intent.GUILD_MESSAGES,
                        Intent.GUILD_VOICE_STATES,
                        Intent.GUILD_MESSAGE_REACTIONS,
                        Intent.DIRECT_MESSAGES,
                        Intent.GUILD_INVITES
                ))
                .withGateway(gateway -> {
                    Launcher.gateway = gateway;

                    // shard-aware resources
                    // services
                    var messageService = new MessageService(gateway, configuration, messageSource);
                    var interactionService = new InteractionService(gateway, configuration, messageService, entityRetriever);

                    var interactionCommandHolder = InteractionCommandHolder.builder()
                            // разное
                            .addCommand(new MathCommand(messageService))
                            .addCommand(new PingCommand(messageService))
                            .addCommand(new AvatarCommand(messageService))
                            .addCommand(new LeetSpeakCommand(messageService))
                            .addCommand(new TextLayoutCommand(messageService))
                            .addCommand(new TransliterationCommand(messageService))
                            // настройки
                            .addCommand(new ActivityCommand(messageService, entityRetriever))
                            .addCommand(new ReactionRolesCommand(messageService, entityRetriever))
                            .addCommand(new StarboardCommand(messageService, entityRetriever))
                            // админские
                            .addCommand(new DeleteCommand(messageService))
                            .build();

                    var handlers = ReactiveEventAdapter.from(
                            new InteractionEventHandler(configuration, interactionCommandHolder, interactionService, entityRetriever),
                            new MessageEventHandler(entityRetriever),
                            new ReactionRoleEventHandler(entityRetriever),
                            new StarboardEventHandler(entityRetriever, messageService));

                    var cmds = interactionCommandHolder.getCommands().values();
                    List<ApplicationCommandRequest> globalCommands = new ArrayList<>();
                    List<ApplicationCommandRequest> guildCommands = new ArrayList<>();
                    for (InteractionCommand value : cmds) {
                        if (value instanceof InteractionGuildCommand) {
                            guildCommands.add(value.getRequest());
                        } else {
                            globalCommands.add(value.getRequest());
                        }
                    }

                    Mono<Void> registerCommands = Mono.zip(gateway.rest().getApplicationId(), Mono.just(globalCommands))
                            .flatMapMany(function((appId, glob) -> gateway.rest().getApplicationService()
                                    .bulkOverwriteGlobalApplicationCommand(appId, glob)))
                            .then(Mono.zip(gateway.rest().getApplicationId(), Mono.just(guildCommands))
                                    .flatMapMany(function((appId, guild) -> gateway.getGuilds()
                                            .flatMap(g -> gateway.rest().getApplicationService()
                                                    .bulkOverwriteGuildApplicationCommand(appId, g.getId().asLong(), guild)
                                                    .map(data -> createOwnerPermissions(g, data))
                                                    .collectList()
                                                    .flatMapMany(p -> gateway.rest().getApplicationService()
                                                            .bulkModifyApplicationCommandPermissions(appId, g.getId().asLong(), p))
                                                    .onErrorResume(e -> e instanceof ClientException, e -> Flux.empty()))))
                                    .then());

                    Mono<Void> registerEvents = gateway.on(handlers)
                            .then();

                    Mono<Void> registerTasks = Flux.just(
                            new ActivityTask(configuration, entityRetriever))
                            .flatMap(task -> Flux.interval(task.getInterval())
                                    .flatMap(l -> task.execute()))
                            .then();

                    return Mono.when(registerEvents, registerCommands, registerTasks)
                            .doFirst(() -> log.info("Bot bootstrapped in {} seconds.", (System.currentTimeMillis() - pre) / 1000f));
                })
                .onErrorStop()
                .doFirst(() -> Signal.handle(new Signal("INT"), sig -> {
                    run(() -> {
                        String json = jacksonResources.getObjectMapper()
                                .writerWithDefaultPrettyPrinter()
                                .writeValueAsString(configuration);

                        Files.writeString(cfg.toPath(), json);
                        log.info("Bot configuration saved.");
                    });
                    run(() -> {
                        if (gateway != null) {
                            gateway.logout().block();
                        }
                    });
                    run(pool::dispose);
                    run(() -> System.exit(1));
                }))
                .block();
    }

    private static PartialGuildApplicationCommandPermissionsData createOwnerPermissions(Guild guild, ApplicationCommandData data){
        return PartialGuildApplicationCommandPermissionsData.builder()
                .id(data.id())
                // разрешаем для владельца сервера
                .addPermissions(ApplicationCommandPermissionsData.builder()
                        .type(2)
                        .id(guild.getOwnerId().asString())
                        .permission(true)
                        .build())
                // запрещаем @everyone
                .addPermissions(ApplicationCommandPermissionsData.builder()
                        .type(1)
                        .id(guild.getId().asString()) // NOTE: не забыть бы, что guild_id == @everyone роль
                        .permission(false)
                        .build())
                .build();
    }

    private static void run(UnsafeRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            log.error("Failed to execute shutdown action.", t);
        }
    }
}
