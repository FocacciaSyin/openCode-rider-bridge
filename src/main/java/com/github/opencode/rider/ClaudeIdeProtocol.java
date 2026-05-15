package com.github.opencode.rider;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ClaudeIdeProtocol {
    public static final String PROTOCOL_VERSION = "2025-11-25";
    public static final String SERVER_NAME = "rider-opencode-context";
    public static final String SERVER_VERSION = "0.1.0";
    private static final Pattern REQUEST_ID = Pattern.compile("\\\"id\\\"\\s*:\\s*(\\d+)");

    private ClaudeIdeProtocol() {
    }

    public static String initializeResponse(int id) {
        return "{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"result\":{" +
            "\"protocolVersion\":" + JsonString.quote(PROTOCOL_VERSION) + "," +
            "\"serverInfo\":{" +
            "\"name\":" + JsonString.quote(SERVER_NAME) + "," +
            "\"version\":" + JsonString.quote(SERVER_VERSION) +
            "}}}";
    }

    public static int extractRequestId(String message) {
        Matcher matcher = REQUEST_ID.matcher(message);
        if (!matcher.find()) {
            return 0;
        }
        return Integer.parseInt(matcher.group(1));
    }
}
