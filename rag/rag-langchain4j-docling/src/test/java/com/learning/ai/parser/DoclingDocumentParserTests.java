package com.learning.ai.parser;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class DoclingDocumentParserTests {

    @Test
    void requestTimesOutWhenDoclingDoesNotRespond() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            Thread serverThread = new Thread(() -> acceptAndHoldConnection(serverSocket));
            serverThread.setDaemon(true);
            serverThread.start();

            DoclingDocumentParser parser = new DoclingDocumentParser(
                    "http://localhost:" + serverSocket.getLocalPort(), Duration.ofSeconds(1), Duration.ofMillis(100));

            assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream("PDF".getBytes(StandardCharsets.UTF_8))))
                    .hasRootCauseInstanceOf(HttpTimeoutException.class);
        }
    }

    private static void acceptAndHoldConnection(ServerSocket serverSocket) {
        try (Socket socket = serverSocket.accept()) {
            Thread.sleep(Duration.ofSeconds(1));
        } catch (Exception ignored) {
            // The client may close the connection when its request timeout expires.
        }
    }
}
