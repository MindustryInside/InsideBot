package inside.openweather.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import discord4j.common.JacksonResources;
import org.junit.jupiter.api.*;
import reactor.util.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DeserializationTest{

    private static final Logger log = Loggers.getLogger(DeserializationTest.class);

    private ObjectMapper mapper;

    @BeforeEach
    public void setUp(){
        mapper = JacksonResources.INITIALIZER.apply(new ObjectMapper())
                .addHandler(new DeserializationProblemHandler(){
                    @Override
                    public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser p,
                                                         JsonDeserializer<?> deserializer, Object beanOrClass,
                                                         String propertyName) throws IOException{
                        log.warn("Unknown property in {}: {}", beanOrClass, propertyName);
                        p.skipChildren();
                        return true;
                    }
                });
    }

    private <T> T read(String from, Class<T> into) throws IOException{
        return mapper.readValue(getClass().getResourceAsStream(from), into);
    }

    @Test
    public void currentWeatherFields() throws IOException{
        CurrentWeatherData currentWeather = read("/openweather/CurrentWeather.json", CurrentWeatherData.class);
        log.info("{}", currentWeather);
    }

    @Test
    public void currentWeatherEquals() throws IOException{
        CurrentWeatherData expected = CurrentWeatherData.builder()
                .coordinate(CoordinateData.of(-0.1257f, 51.5085f))
                .addWeather(WeatherData.builder()
                        .id(801)
                        .main("Clouds")
                        .description("few clouds")
                        .icon("02n")
                        .build())
                .base("stations")
                .main(MainData.builder()
                        .temperature(286.88f)
                        .feelsLike(286.76f)
                        .temperatureMin(285.1f)
                        .temperatureMax(288.25f)
                        .pressure(1015)
                        .humidity(94)
                        .build())
                .visibility(10000)
                .wind(WindData.builder()
                        .speed(0.45f)
                        .degrees(254)
                        .gust(3.58f)
                        .build())
                .clouds(CloudsData.of(23))
                .dateTime(1628564545)
                .system(SystemData.builder()
                        .type(2)
                        .id(2019646)
                        .country("GB")
                        .sunrise(1628570299)
                        .sunset(1628624011)
                        .build())
                .timezone(3600)
                .id(2643743)
                .name("London")
                .code(200)
                .build();

        CurrentWeatherData currentWeather = read("/openweather/CurrentWeather.json", CurrentWeatherData.class);
        assertEquals(expected, currentWeather);
    }
}
