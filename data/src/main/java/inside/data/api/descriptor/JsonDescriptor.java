package inside.data.api.descriptor;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import discord4j.common.JacksonResources;
import io.r2dbc.postgresql.codec.Json;
import reactor.core.Exceptions;
import reactor.util.annotation.Nullable;

import java.lang.reflect.Type;
import java.util.Objects;

public class JsonDescriptor extends BaseDescriptor<Object> {

    private final JacksonResources jacksonResources;
    private final JavaType javaType;

    public JsonDescriptor(JacksonResources jacksonResources, Type type) {
        this.jacksonResources = Objects.requireNonNull(jacksonResources, "jacksonResources");
        Objects.requireNonNull(type, "type");
        javaType = jacksonResources.getObjectMapper().constructType(type);
    }

    @Override
    public Class<?> getSqlType() {
        return Json.class;
    }

    @Nullable
    @Override
    public Object unwrap(Object value) {
        try {
            return Json.of(jacksonResources.getObjectMapper().writeValueAsString(value));
        } catch (Throwable t) {
            throw Exceptions.propagate(t);
        }
    }

    @Override
    public Object wrap(@Nullable Object value) {
        if (value == null) {
            try {
                JsonNode nullNode = jacksonResources.getObjectMapper().nullNode();

                return jacksonResources.getObjectMapper().convertValue(nullNode, javaType);
            } catch (Throwable t) {
                throw Exceptions.propagate(t);
            }
        }

        if (value instanceof Json j) {
            try {
                return jacksonResources.getObjectMapper().readValue(j.asArray(), javaType);
            } catch (Throwable t) {
                throw Exceptions.propagate(t);
            }
        }

        throw unknownWrap(value.getClass());
    }
}
