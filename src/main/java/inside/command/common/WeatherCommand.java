package inside.command.common;

import discord4j.common.ReactorResources;
import discord4j.common.util.TimestampFormat;
import inside.Settings;
import inside.command.Command;
import inside.command.model.*;
import inside.openweather.json.CurrentWeatherData;
import inside.util.Lazy;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Instant;
import java.util.Map;

import static inside.util.ContextUtil.*;

@DiscordCommand(key = "weather", params = "command.weather.params", description = "command.weather.description")
public class WeatherCommand extends Command{

    private static final float mmHg = 133.322f;
    private static final float hPa = 100;

    private final Lazy<HttpClient> httpClient = Lazy.of(ReactorResources.DEFAULT_HTTP_CLIENT);

    @Autowired
    private Settings settings;

    @Override
    public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
        String city = interaction.getOption(0)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString)
                .orElseThrow(IllegalStateException::new);

        Map<String, Object> query = Map.of(
                "q", city,
                "units", "metric",
                "lang", env.context().get(KEY_LOCALE).toString(),
                "appid", settings.getDiscord().getOpenweatherApiKey()
        );

        return httpClient.get().get().uri(CommandUtils.expandQuery("api.openweathermap.org/data/2.5/weather", query))
                .responseSingle((res, buf) -> res.status() == HttpResponseStatus.NOT_FOUND
                        ? messageService.err(env, "command.weather.not-found").then(Mono.never())
                        : buf.asString())
                .flatMap(str -> Mono.fromCallable(() ->
                        env.message().getClient().rest().getCoreResources().getJacksonResources()
                                .getObjectMapper().readValue(str, CurrentWeatherData.class)))
                .flatMap(data -> messageService.infoTitled(env, data.name(),
                        "command.weather.format", data.weather().get(0).description(),
                        data.main().temperature(), data.main().feelsLike(),
                        data.main().temperatureMin(), data.main().temperatureMax(),
                        data.main().pressure() * hPa / mmHg, data.main().humidity(),
                        data.visibility(), data.clouds().all(), data.wind().speed(),
                        TimestampFormat.LONG_DATE_TIME.format(Instant.ofEpochSecond(data.dateTime())))
                        .withMessageReference(env.message().getId()))
                .then();
    }
}
