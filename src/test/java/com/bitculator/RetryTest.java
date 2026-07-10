package com.bitculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bitculator.StubServer.Reply;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RetryTest {

    private static Bitculator client(StubServer server) {
        return Bitculator.builder("test-key")
                .baseUrl(server.baseUrl())
                .userAgent("bitculator-java/test")
                .build();
    }

    @Test
    void getRetriesA5xxAndSucceeds() throws Exception {
        try (StubServer server = new StubServer(Reply.error(500), Reply.ok())) {
            assertEquals("ok", client(server).coins().get("bitcoin", Params.of()).asText());
            assertEquals(2, server.hits());
        }
    }

    @Test
    void deleteRetriesA5xxAndSucceeds() throws Exception {
        try (StubServer server = new StubServer(Reply.error(500), Reply.ok())) {
            client(server).webhooks().delete(7);
            assertEquals(2, server.hits());
        }
    }

    @Test
    void postNeverRetriesA5xx() throws Exception {
        try (StubServer server = new StubServer(Reply.error(500), Reply.ok())) {
            Bitculator client = client(server);
            ApiException e = assertThrows(ApiException.class,
                    () -> client.editorial().submitArticleFeedback("x", true));
            assertTrue(e.isServer());
            assertEquals(1, server.hits(), "a POST must never be re-sent on a 5xx");
        }
    }

    @Test
    void postDoesRetryA429() throws Exception {
        try (StubServer server = new StubServer(Reply.error(429), Reply.ok())) {
            client(server).editorial().submitArticleFeedback("x", true);
            assertEquals(2, server.hits());
        }
    }

    @Test
    void givesUpAfterMaxRetries() throws Exception {
        try (StubServer server = new StubServer(Reply.error(500), Reply.error(500), Reply.error(500), Reply.ok())) {
            Bitculator client = client(server);
            ApiException e = assertThrows(ApiException.class, () -> client.coins().get("bitcoin", Params.of()));
            assertTrue(e.isServer());
            assertEquals(3, server.hits(), "initial attempt + 2 retries; the queued 200 is never reached");
        }
    }

    @Test
    void honorsRetryAfterSecondsOnA429() throws Exception {
        try (StubServer server = new StubServer(Reply.error(429).header("Retry-After", "1"), Reply.ok())) {
            long started = System.nanoTime();
            client(server).coins().get("bitcoin", Params.of());
            long elapsedMs = (System.nanoTime() - started) / 1_000_000;
            assertTrue(elapsedMs >= 900, "Retry-After not honored; elapsed " + elapsedMs + "ms");
            assertEquals(2, server.hits());
        }
    }

    @Test
    void timedOutGetRetriesThenThrows() throws Exception {
        try (StubServer server = new StubServer(
                Reply.ok().delay(500),
                Reply.ok().delay(500))) {
            Bitculator client = Bitculator.builder("test-key")
                    .baseUrl(server.baseUrl())
                    .timeout(Duration.ofMillis(100))
                    .maxRetries(1)
                    .build();
            assertThrows(TimeoutException.class, () -> client.coins().get("bitcoin", Params.of()));
            // The timeout is a transport failure, so the idempotent GET retried once.
            assertEquals(2, server.hits());
        }
    }

    @Test
    void timedOutPostFailsWithoutRetrying() throws Exception {
        try (StubServer server = new StubServer(Reply.ok().delay(500), Reply.ok())) {
            Bitculator client = Bitculator.builder("test-key")
                    .baseUrl(server.baseUrl())
                    .timeout(Duration.ofMillis(100))
                    .build();
            assertThrows(TimeoutException.class, () -> client.editorial().submitArticleFeedback("x", true));
            // Give a wrongly-issued retry time to land before asserting it never came.
            Thread.sleep(700);
            assertEquals(1, server.hits(), "a POST must never be re-sent on a timeout");
        }
    }
}
