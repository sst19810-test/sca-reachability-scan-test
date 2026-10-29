package demo.sast.cross;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class ShellRunner {
    // CWE-78: AdminService builds the command line from request data
    public String run(String commandLine) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("sh", "-c", commandLine).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        process.waitFor();
        return output;
    }
}
