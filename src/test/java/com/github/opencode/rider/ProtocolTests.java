package com.github.opencode.rider;

import java.util.List;

public final class ProtocolTests {
    public static void main(String[] args) {
        lockFileJsonIncludesTransportWorkspaceAndOptionalAuthToken();
        selectionChangedUsesOneBasedLinesAndJsonRpcShape();
        initializeResponseEchoesRequestId();
        extractsNumericRequestId();
    }

    private static void lockFileJsonIncludesTransportWorkspaceAndOptionalAuthToken() {
        String json = ClaudeIdeLockFilePayload.toJson(List.of("C:\\repo\\project"), "secret-token");

        assertContains(json, "\"transport\":\"ws\"");
        assertContains(json, "\"authToken\":\"secret-token\"");
        assertContains(json, "\"workspaceFolders\":[\"C:\\\\repo\\\\project\"]");
    }

    private static void selectionChangedUsesOneBasedLinesAndJsonRpcShape() {
        String json = new SelectionPayload(
            "C:\\repo\\project\\Foo.cs",
            "Console.WriteLine();",
            9,
            4,
            9,
            24
        ).toJsonRpcNotification();

        assertContains(json, "\"jsonrpc\":\"2.0\"");
        assertContains(json, "\"method\":\"selection_changed\"");
        assertContains(json, "\"filePath\":\"C:\\\\repo\\\\project\\\\Foo.cs\"");
        assertContains(json, "\"text\":\"Console.WriteLine();\"");
        assertContains(json, "\"start\":{\"line\":10,\"character\":4}");
        assertContains(json, "\"end\":{\"line\":10,\"character\":24}");
    }

    private static void initializeResponseEchoesRequestId() {
        String json = ClaudeIdeProtocol.initializeResponse(7);

        assertContains(json, "\"jsonrpc\":\"2.0\"");
        assertContains(json, "\"id\":7");
        assertContains(json, "\"protocolVersion\":\"2025-11-25\"");
        assertContains(json, "\"name\":\"rider-opencode-context\"");
    }

    private static void extractsNumericRequestId() {
        int id = ClaudeIdeProtocol.extractRequestId("{\"jsonrpc\":\"2.0\",\"id\":42,\"method\":\"initialize\"}");

        if (id != 42) {
            throw new AssertionError("Expected request id 42 but got " + id);
        }
    }

    private static void assertContains(String text, String expected) {
        if (!text.contains(expected)) {
            throw new AssertionError("Expected to find " + expected + " in " + text);
        }
    }
}
