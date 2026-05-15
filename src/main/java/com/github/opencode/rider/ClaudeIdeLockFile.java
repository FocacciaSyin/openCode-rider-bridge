package com.github.opencode.rider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ClaudeIdeLockFile implements AutoCloseable {
    private final Path path;

    private ClaudeIdeLockFile(Path path) {
        this.path = path;
    }

    public static ClaudeIdeLockFile create(Path directory, int port, List<String> workspaceFolders, String authToken)
        throws IOException {
        Files.createDirectories(directory);
        Path path = directory.resolve(port + ".lock");
        Files.writeString(path, ClaudeIdeLockFilePayload.toJson(workspaceFolders, authToken), StandardCharsets.UTF_8);
        return new ClaudeIdeLockFile(path);
    }

    @Override
    public void close() throws IOException {
        Files.deleteIfExists(path);
    }
}
