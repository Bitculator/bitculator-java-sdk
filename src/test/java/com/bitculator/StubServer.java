package com.bitculator;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;

/**
 * A local stub server on the JDK's built-in {@link HttpServer}: shifts one
 * queued {@link Reply} per request and records each request — the tests drive
 * the SDK's real transport with no mocks and no test-framework HTTP deps.
 */
final class StubServer implements AutoCloseable {

    record Recorded(String method, URI uri, Map<String, String> headers, String body) {
        String header(String name) {
            return headers.get(name.toLowerCase());
        }

        String path() {
            return uri.getRawPath();
        }

        boolean hasQueryPair(String pair) {
            String query = uri.getRawQuery();
            return query != null && List.of(query.split("&")).contains(pair);
        }
    }

    static final class Reply {
        private final int status;
        private final String body;
        private final Map<String, String> headers = new LinkedHashMap<>();
        private long delayMs;

        private Reply(int status, String body) {
            this.status = status;
            this.body = body;
        }

        static Reply json(int status, String body) {
            return new Reply(status, body);
        }

        static Reply error(int status) {
            return new Reply(status, "{\"error\":{\"code\":\"err_" + status + "\",\"message\":\"stub error " + status + "\",\"details\":null}}");
        }

        static Reply ok() {
            return new Reply(200, "{\"data\":\"ok\"}");
        }

        Reply header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        Reply delay(long ms) {
            this.delayMs = ms;
            return this;
        }
    }

    private final HttpServer server;
    private final ConcurrentLinkedDeque<Reply> replies = new ConcurrentLinkedDeque<>();
    private final List<Recorded> requests = Collections.synchronizedList(new ArrayList<>());

    StubServer(Reply... queued) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        replies.addAll(List.of(queued));
        server.createContext("/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
            Map<String, String> headers = new HashMap<>();
            exchange.getRequestHeaders().forEach((name, values) -> headers.put(name.toLowerCase(), values.get(0)));
            requests.add(new Recorded(exchange.getRequestMethod(), exchange.getRequestURI(), headers, body));

            Reply reply = replies.pollFirst();
            if (reply == null) {
                reply = Reply.error(599);
            }
            if (reply.delayMs > 0) {
                try {
                    Thread.sleep(reply.delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] payload = reply.body.getBytes(UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            reply.headers.forEach((name, value) -> exchange.getResponseHeaders().set(name, value));
            exchange.sendResponseHeaders(reply.status, payload.length == 0 ? -1 : payload.length);
            if (payload.length > 0) {
                exchange.getResponseBody().write(payload);
            }
            exchange.close();
        });
        // Concurrent handling: a delayed reply must not stop the next request
        // from being recorded the moment it arrives (timeout-retry tests).
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    int hits() {
        return requests.size();
    }

    Recorded request(int index) {
        return requests.get(index);
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
