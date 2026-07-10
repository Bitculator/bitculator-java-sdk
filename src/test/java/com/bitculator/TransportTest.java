package com.bitculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bitculator.StubServer.Reply;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

class TransportTest {

    private static Bitculator client(StubServer server) {
        return Bitculator.builder("test-key")
                .baseUrl(server.baseUrl())
                .userAgent("bitculator-java/test")
                .build();
    }

    @Test
    void sendsBearerAuthAcceptAndUserAgent() throws Exception {
        try (StubServer server = new StubServer(Reply.json(200, "{\"data\":{}}"))) {
            client(server).coins().get("bitcoin", Params.of());
            StubServer.Recorded request = server.request(0);
            assertEquals("Bearer test-key", request.header("authorization"));
            assertEquals("application/json", request.header("accept"));
            assertEquals("bitculator-java/test", request.header("user-agent"));
            assertNull(request.header("content-type"), "GET must not send a Content-Type");
        }
    }

    @Test
    void postSendsAJsonBodyWithContentType() throws Exception {
        try (StubServer server = new StubServer(Reply.json(200, "{\"data\":{}}"))) {
            client(server).editorial().submitArticleFeedback("bitcoin-halving", true);
            StubServer.Recorded request = server.request(0);
            assertEquals("POST", request.method());
            assertEquals("application/json", request.header("content-type"));
            assertEquals("{\"helpful\":true}", request.body());
            assertEquals("/api/v1/articles/bitcoin-halving/feedback", request.path());
        }
    }

    @Test
    void bodylessPostSendsNoContentType() throws Exception {
        try (StubServer server = new StubServer(Reply.ok())) {
            client(server).webhooks().test(7);
            StubServer.Recorded request = server.request(0);
            assertEquals("POST", request.method());
            assertEquals("/api/v1/webhooks/7/test", request.path());
            assertNull(request.header("content-type"));
            assertEquals("", request.body());
        }
    }

    @Test
    void unwrapsTheDataEnvelope() throws Exception {
        try (StubServer server = new StubServer(Reply.json(200, "{\"data\":{\"name\":\"Bitcoin\",\"price\":\"67412.83451201\"}}"))) {
            JsonNode coin = client(server).coins().get("bitcoin", Params.of());
            assertEquals("Bitcoin", coin.get("name").asText());
            assertEquals("67412.83451201", coin.get("price").asText()); // decimal STRING, not a float
        }
    }

    @Test
    void passesThroughBodiesWithoutADataKey() throws Exception {
        try (StubServer server = new StubServer(Reply.json(200, "{\"openapi\":\"3.0.0\"}"))) {
            JsonNode spec = client(server).coins().get("anything", Params.of());
            assertEquals("3.0.0", spec.get("openapi").asText());
        }
    }

    @Test
    void empty2xxBodyIsANullNode() throws Exception {
        try (StubServer server = new StubServer(Reply.json(204, ""))) {
            JsonNode gone = client(server).webhooks().delete(7);
            assertTrue(gone.isNull());
            assertEquals("DELETE", server.request(0).method());
            assertEquals("/api/v1/webhooks/7", server.request(0).path());
        }
    }

    @Test
    void malformed2xxBodyThrowsADecodeError() throws Exception {
        try (StubServer server = new StubServer(Reply.json(200, "<html>gateway page</html>"))) {
            Bitculator client = client(server);
            BitculatorException e = assertThrows(BitculatorException.class,
                    () -> client.coins().get("bitcoin", Params.of()));
            assertTrue(e.getMessage().contains("Failed to decode response"), e.getMessage());
        }
    }

    @Test
    void parsesQuotaHeaders() throws Exception {
        try (StubServer server = new StubServer(Reply.json(200, "{\"data\":[]}")
                .header("X-Quota-Limit", "10000")
                .header("X-Quota-Remaining", "9871")
                .header("X-Quota-Reset", "1720569600"))) {
            Bitculator client = client(server);
            assertNull(client.quota(), "quota must be null before the first call");
            client.coins().get("bitcoin", Params.of());
            Quota quota = client.quota();
            assertEquals(10000, quota.limit());
            assertEquals(9871, quota.remaining());
            assertEquals(1720569600, quota.reset());
            assertEquals("10000", quota.raw().get("x-quota-limit"));
        }
    }

    @Test
    void missingQuotaHeadersParseToNull() throws Exception {
        try (StubServer server = new StubServer(Reply.json(200, "{\"data\":[]}"))) {
            Bitculator client = client(server);
            client.coins().get("bitcoin", Params.of());
            Quota quota = client.quota();
            assertNull(quota.limit());
            assertNull(quota.remaining());
            assertNull(quota.reset());
            assertTrue(quota.raw().isEmpty());
        }
    }

    @Test
    void paramsSerializeCsvBoolsAndSkipNulls() throws Exception {
        try (StubServer server = new StubServer(Reply.json(200, "{\"data\":{}}"))) {
            client(server).coins().get("bitcoin", Params.of()
                    .set("symbols", java.util.List.of("btc", "eth", "sol"))
                    .set("include_inactive", true)
                    .set("locale", null)
                    .set("empty", java.util.List.of())
                    .set("page", 1));
            StubServer.Recorded request = server.request(0);
            assertTrue(request.hasQueryPair("symbols=btc%2Ceth%2Csol"), request.uri().toString());
            assertTrue(request.hasQueryPair("include_inactive=true"), request.uri().toString());
            assertTrue(request.hasQueryPair("page=1"), request.uri().toString());
            assertFalse(request.uri().toString().contains("locale"), request.uri().toString());
            assertFalse(request.uri().toString().contains("empty"), request.uri().toString());
        }
    }

    @Test
    void floatsGoOnTheWireAsPlainDecimals() throws Exception {
        try (StubServer server = new StubServer(Reply.json(200, "{\"data\":{}}"))) {
            client(server).coins().get("bitcoin", Params.of()
                    .set("min_price", 1e-7)
                    .set("max_price", -2.5e-8)
                    .set("min_marketcap", 1e21));
            StubServer.Recorded request = server.request(0);
            assertTrue(request.hasQueryPair("min_price=0.0000001"), request.uri().toString());
            assertTrue(request.hasQueryPair("max_price=-0.000000025"), request.uri().toString());
            assertTrue(request.hasQueryPair("min_marketcap=1000000000000000000000"), request.uri().toString());
        }
    }
}
