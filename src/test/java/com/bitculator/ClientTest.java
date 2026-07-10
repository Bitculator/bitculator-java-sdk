package com.bitculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bitculator.StubServer.Reply;
import org.junit.jupiter.api.Test;

class ClientTest {

    @Test
    void requiresAnApiKey() {
        assertThrows(BitculatorException.class, () -> new Bitculator(""));
        assertThrows(BitculatorException.class, () -> Bitculator.builder(null));
    }

    @Test
    void exposesAll15ResourceGroups() {
        Bitculator client = new Bitculator("k");
        Object[] groups = {
                client.coins(), client.prices(), client.markets(), client.exchanges(), client.wallets(),
                client.globalMarket(), client.sentiment(), client.indicators(), client.liquidations(),
                client.conversion(), client.calculators(), client.editorial(), client.alarms(),
                client.webhooks(), client.meta(),
        };
        assertEquals(15, groups.length);
        for (Object group : groups) {
            assertNotNull(group);
        }
    }

    @Test
    void percentEncodesDynamicPathSegments() throws Exception {
        try (StubServer server = new StubServer(Reply.json(200, "{\"data\":{}}"))) {
            Bitculator client = Bitculator.builder("k").baseUrl(server.baseUrl()).build();
            client.coins().get("weird/slug name", Params.of());
            assertEquals("/api/v1/coins/weird%2Fslug%20name", server.request(0).path());
        }
    }

    @Test
    void baseUrlTrimsTrailingSlashes() throws Exception {
        try (StubServer server = new StubServer(Reply.json(200, "{\"data\":{}}"))) {
            Bitculator client = Bitculator.builder("k").baseUrl(server.baseUrl() + "///").build();
            client.coins().get("bitcoin", Params.of());
            assertEquals("/api/v1/coins/bitcoin", server.request(0).path());
        }
    }

    @Test
    void defaultUserAgentCarriesTheVersion() throws Exception {
        try (StubServer server = new StubServer(Reply.json(200, "{\"data\":{}}"))) {
            Bitculator client = Bitculator.builder("k").baseUrl(server.baseUrl()).build();
            client.coins().get("bitcoin", Params.of());
            String userAgent = server.request(0).header("user-agent");
            assertTrue(userAgent.startsWith("bitculator-java/"), userAgent);
            assertTrue(userAgent.contains(Bitculator.VERSION), userAgent);
        }
    }
}
