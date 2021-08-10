package inside.openweather.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import discord4j.common.JacksonResources;
import org.junit.jupiter.api.*;
import reactor.util.*;

import java.io.IOException;

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
    public void currentWeather() throws IOException{
        CurrentWeatherData currentWeather = read("/openweather/CurrentWeather.json", CurrentWeatherData.class);
        log.info("{}", currentWeather);
    }
}
