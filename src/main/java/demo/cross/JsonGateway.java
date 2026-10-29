package demo.cross;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.ext.NioPathDeserializer;
import java.nio.file.Path;
import java.io.IOException;

/** Shared Jackson sink: the caller and target model live in other source files. */
public final class JsonGateway {
    private final ObjectMapper mapper = new ObjectMapper();

    public Path deserializePath(String json) throws IOException {
        try (JsonParser parser = mapper.createParser(json)) {
            parser.nextToken();
            DefaultDeserializationContext context =
                    ((DefaultDeserializationContext) mapper.getDeserializationContext())
                            .createInstance(mapper.getDeserializationConfig(), parser,
                                    mapper.getInjectableValues());
            return new NioPathDeserializer().deserialize(parser, context);
        }
    }

    public <T> T read(String json, Class<T> type) throws IOException {
        return mapper.readValue(json, type);
    }
}
