package com.github.opencode.rider;

public final class SelectionPayload {
    private final String filePath;
    private final String text;
    private final int startLineZeroBased;
    private final int startCharacter;
    private final int endLineZeroBased;
    private final int endCharacter;

    public SelectionPayload(
        String filePath,
        String text,
        int startLineZeroBased,
        int startCharacter,
        int endLineZeroBased,
        int endCharacter
    ) {
        this.filePath = filePath;
        this.text = text;
        this.startLineZeroBased = startLineZeroBased;
        this.startCharacter = startCharacter;
        this.endLineZeroBased = endLineZeroBased;
        this.endCharacter = endCharacter;
    }

    public String toJsonRpcNotification() {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"selection_changed\",\"params\":{" +
            "\"filePath\":" + JsonString.quote(filePath) + "," +
            "\"ranges\":[{" +
            "\"text\":" + JsonString.quote(text) + "," +
            "\"selection\":{" +
            "\"start\":{" +
            "\"line\":" + (startLineZeroBased + 1) + "," +
            "\"character\":" + startCharacter +
            "}," +
            "\"end\":{" +
            "\"line\":" + (endLineZeroBased + 1) + "," +
            "\"character\":" + endCharacter +
            "}}}]}}";
    }
}
