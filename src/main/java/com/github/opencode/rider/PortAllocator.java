package com.github.opencode.rider;

import java.io.IOException;
import java.net.ServerSocket;

public final class PortAllocator {
    private PortAllocator() {
    }

    public static int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }
}
