package demo.sast.cross;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DocumentStore {
    private static final Path ROOT = Path.of("/var/app/notes");

    // CWE-22: "../" in name escapes ROOT, allowing arbitrary file writes
    public void write(String name, String body) throws IOException {
        Files.writeString(ROOT.resolve(name), body);
    }
}
