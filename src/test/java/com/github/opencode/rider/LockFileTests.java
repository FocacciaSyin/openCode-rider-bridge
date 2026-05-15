package com.github.opencode.rider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class LockFileTests {
    public static void main(String[] args) throws Exception {
        writesLockFileNamedAfterPortAndDeletesItOnClose();
    }

    private static void writesLockFileNamedAfterPortAndDeletesItOnClose() throws Exception {
        Path directory = Files.createTempDirectory("opencode-rider-lock");
        ClaudeIdeLockFile lockFile = ClaudeIdeLockFile.create(directory, 49152, List.of("C:\\repo\\project"), "token");
        Path path = directory.resolve("49152.lock");

        if (!Files.exists(path)) {
            throw new AssertionError("Expected lock file to exist: " + path);
        }

        String json = Files.readString(path);
        if (!json.contains("\"workspaceFolders\":[\"C:\\\\repo\\\\project\"]")) {
            throw new AssertionError("Expected workspace folder in lock file: " + json);
        }

        lockFile.close();
        if (Files.exists(path)) {
            throw new AssertionError("Expected lock file to be deleted: " + path);
        }
    }
}
