package com.bitculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bitculator.StubServer.Reply;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class ErrorsTest {

    /** A client that never retries, so every status maps straight to its exception. */
    private static Bitculator oneShot(StubServer server) {
        return Bitculator.builder("test-key")
                .baseUrl(server.baseUrl())
                .userAgent("bitculator-java/test")
                .maxRetries(0)
                .build();
    }

    private static ApiException failing(StubServer server) {
        Bitculator client = oneShot(server);
        return assertThrows(ApiException.class, () -> client.coins().get("bitcoin", Params.of()));
    }

    @Test
    void mapsStatusesToPredicates() throws Exception {
        record Case(int status, Predicate<ApiException> predicate, String name) {}
        Case[] cases = {
                new Case(401, ApiException::isAuth, "isAuth"),
                new Case(403, ApiException::isPermission, "isPermission"),
                new Case(404, ApiException::isNotFound, "isNotFound"),
                new Case(422, ApiException::isValidation, "isValidation"),
                new Case(429, ApiException::isRateLimited, "isRateLimited"),
                new Case(500, ApiException::isServer, "isServer"),
                new Case(503, ApiException::isServer, "isServer"),
        };
        for (Case c : cases) {
            try (StubServer server = new StubServer(Reply.error(c.status()))) {
                ApiException e = failing(server);
                assertEquals(c.status(), e.status());
                assertTrue(c.predicate().test(e), c.name() + " should be true for " + c.status());
                assertEquals("err_" + c.status(), e.code());
                assertTrue(e instanceof BitculatorException, "ApiException derives from BitculatorException");
            }
        }
    }

    @Test
    void carriesCodeMessageAndDetailsFromTheEnvelope() throws Exception {
        String body = "{\"error\":{\"code\":\"validation_failed\",\"message\":\"Invalid parameters.\","
                + "\"details\":{\"per_page\":[\"must not be greater than 100\"]}}}";
        try (StubServer server = new StubServer(Reply.json(422, body))) {
            ApiException e = failing(server);
            assertEquals("validation_failed", e.code());
            assertEquals("Invalid parameters.", e.getMessage());
            assertEquals("must not be greater than 100", e.details().get("per_page").get(0).asText());
        }
    }

    @Test
    void fallsBackToTopLevelMessageThenHttpStatus() throws Exception {
        try (StubServer server = new StubServer(Reply.json(500, "{\"message\":\"Whoops.\"}"))) {
            assertEquals("Whoops.", failing(server).getMessage());
        }
        try (StubServer server = new StubServer(Reply.json(502, "Bad Gateway"))) {
            ApiException e = failing(server);
            assertEquals("HTTP 502", e.getMessage());
            assertNull(e.code());
        }
    }

    @Test
    void capturesRequestIdAndRetryAfter() throws Exception {
        try (StubServer server = new StubServer(Reply.error(500).header("X-Request-Id", "req_123"))) {
            assertEquals("req_123", failing(server).requestId());
        }
        try (StubServer server = new StubServer(Reply.error(429).header("Retry-After", "30"))) {
            assertEquals(30, failing(server).retryAfter());
        }
        try (StubServer server = new StubServer(Reply.error(429))) {
            assertNull(failing(server).retryAfter());
        }
    }
}
