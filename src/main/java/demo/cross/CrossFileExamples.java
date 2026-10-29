package demo.cross;

import javax.xml.datatype.Duration;
import java.io.IOException;
import java.nio.file.Path;

/** Input -> application service -> separate gateway -> library implementation. */
public final class CrossFileExamples {
    private final JsonGateway jsonGateway = new JsonGateway();
    private final BufferGateway bufferGateway = new BufferGateway();

    public Path readPath(String json) throws IOException {
        return jsonGateway.deserializePath(json);
    }

    public Duration readDuration(String json) throws IOException {
        return jsonGateway.read(json, Duration.class);
    }

    public Event readEvent(String json) throws IOException {
        return jsonGateway.read(json, Event.class);
    }

    public byte[] writePadding(int count) throws IOException {
        return bufferGateway.writePadding(count);
    }
}
