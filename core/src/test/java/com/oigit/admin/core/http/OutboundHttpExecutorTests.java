package com.oigit.admin.core.http;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OutboundHttpExecutorTests {

    private HttpServer server;
    private OutboundHttpExecutor executor;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        executor = new OutboundHttpExecutor(
                new OutboundHttpClientFactory(new OutboundHttpProxyProperties()));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void shouldReadShortResponseWithoutRequiringTheMaximumByteCount() throws Exception {
        byte[] body = "{\"errcode\":0}".getBytes(StandardCharsets.UTF_8);
        respond("/short", body);

        OutboundHttpResponse response = executor.execute(
                options(), OutboundHttpRequests.get(url("/short")), 64 * 1024);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo(body);
        assertThat(response.bodyTooLarge()).isFalse();
    }

    @Test
    void shouldStopAfterOneByteBeyondConfiguredResponseLimit() throws Exception {
        respond("/large", "123456".getBytes(StandardCharsets.UTF_8));

        OutboundHttpResponse response = executor.execute(
                options(), OutboundHttpRequests.get(url("/large")), 4);

        assertThat(response.bodyTooLarge()).isTrue();
        assertThat(response.body()).isEmpty();
    }

    private void respond(String path, byte[] body) {
        server.createContext(path, exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
    }

    private String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    private OutboundHttpClientOptions options() {
        return OutboundHttpClientOptions.strict(
                Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(2));
    }
}
