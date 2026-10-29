package demo.single;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.ext.NioPathDeserializer;

import org.postgresql.util.internal.PgBufferedOutputStream;
import javax.xml.datatype.Duration;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

/** All application call sites and models are contained in this one source file. */
public final class SingleFileExamples {
    private final ObjectMapper mapper = new ObjectMapper();

    // Direct invocation of the library method named in the supplied report.
    public Path readPath(String json) throws IOException {
        try (JsonParser parser = mapper.createParser(json)) {
            parser.nextToken();
            DefaultDeserializationContext context =
                    ((DefaultDeserializationContext) mapper.getDeserializationContext())
                            .createInstance(mapper.getDeserializationConfig(), parser,
                                    mapper.getInjectableValues());
            return new NioPathDeserializer().deserialize(parser, context);
        }
    }

    // readValue -> CoreXMLDeserializers.Std._deserialize
    public Duration readDuration(String json) throws IOException {
        return mapper.readValue(json, Duration.class);
    }

    // readValue -> TypeDeserializerBase._findDeserializer (unknown NAME -> defaultImpl)
    public Event readEvent(String json) throws IOException {
        return mapper.readValue(json, Event.class);
    }

    // Direct call to the exact driver method named in the attachment.
    public byte[] writePadding(int count) throws IOException {
        if (count < 0 || count > 64) throw new IllegalArgumentException("count must be 0..64");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PgBufferedOutputStream stream = new PgBufferedOutputStream(bytes, 128)) {
            stream.writeZeros(count);
        }
        return bytes.toByteArray();
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", defaultImpl = Event.class)
    public static class Event {
        public String message;
    }
}
