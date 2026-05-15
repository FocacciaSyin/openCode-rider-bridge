package com.github.opencode.rider;

import java.util.List;
import java.util.stream.Collectors;

public final class ClaudeIdeLockFilePayload {
    private ClaudeIdeLockFilePayload() {
    }

    public static String toJson(List<String> workspaceFolders, String authToken) {
        String folders = workspaceFolders.stream().map(JsonString::quote).collect(Collectors.joining(","));
        String auth = authToken == null || authToken.isBlank() ? "" : ",\"authToken\":" + JsonString.quote(authToken);
        return "{\"transport\":\"ws\"" + auth + ",\"workspaceFolders\":[" + folders + "]}";
    }
}
