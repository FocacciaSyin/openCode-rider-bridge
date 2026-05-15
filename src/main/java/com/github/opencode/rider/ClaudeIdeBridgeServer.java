package com.github.opencode.rider;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ClaudeIdeBridgeServer extends WebSocketServer {
    private static final String AUTH_HEADER = "x-claude-code-ide-authorization";

    private final String authToken;
    private final Set<WebSocket> clients = ConcurrentHashMap.newKeySet();

    public ClaudeIdeBridgeServer(int port, String authToken) {
        super(new InetSocketAddress("127.0.0.1", port));
        this.authToken = authToken;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        if (authToken != null && !authToken.equals(handshake.getFieldValue(AUTH_HEADER))) {
            conn.close(1008, "Unauthorized");
            return;
        }
        clients.add(conn);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        clients.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (message.contains("\"method\":\"initialize\"") || message.contains("\"method\" : \"initialize\"")) {
            conn.send(ClaudeIdeProtocol.initializeResponse(ClaudeIdeProtocol.extractRequestId(message)));
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (conn != null) {
            clients.remove(conn);
        }
    }

    @Override
    public void onStart() {
    }

    public void broadcastSelection(SelectionPayload payload) {
        String message = payload.toJsonRpcNotification();
        for (WebSocket client : clients) {
            if (client.isOpen()) {
                client.send(message);
            }
        }
    }
}
