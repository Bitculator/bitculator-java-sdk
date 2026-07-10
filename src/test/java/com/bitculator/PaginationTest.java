package com.bitculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bitculator.StubServer.Reply;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaginationTest {

    private static Bitculator client(StubServer server) {
        return Bitculator.builder("test-key")
                .baseUrl(server.baseUrl())
                .userAgent("bitculator-java/test")
                .build();
    }

    private static Reply pageReply(int page, int last) {
        int first = (page - 1) * 2;
        String next = page < last ? "\"/api/v1/coins?page=" + (page + 1) + "\"" : "null";
        return Reply.json(200, "{\"data\":[{\"id\":" + (first + 1) + "},{\"id\":" + (first + 2) + "}],"
                + "\"meta\":{\"current_page\":" + page + ",\"last_page\":" + last + ",\"per_page\":2,\"total\":" + last * 2 + "},"
                + "\"links\":{\"next\":" + next + "}}");
    }

    @Test
    void pageExposesDataMetaAndHasMore() throws Exception {
        try (StubServer server = new StubServer(pageReply(1, 3))) {
            Page page = client(server).coins().list(Params.of().set("per_page", 2));
            assertEquals(2, page.data().size());
            assertEquals(1, page.meta().currentPage());
            assertEquals(3, page.meta().lastPage());
            assertTrue(page.hasMore());
        }
    }

    @Test
    void nextPageRequestsTheFollowingPageAndPreservesTheQuery() throws Exception {
        try (StubServer server = new StubServer(pageReply(1, 3), pageReply(2, 3))) {
            Page first = client(server).coins().list(Params.of().set("per_page", 2).set("sort", "rank"));
            Page second = first.nextPage();
            assertEquals(3, second.data().get(0).get("id").asInt());
            StubServer.Recorded request = server.request(1);
            for (String pair : new String[] {"page=2", "per_page=2", "sort=rank"}) {
                assertTrue(request.hasQueryPair(pair), "second query missing " + pair + ": " + request.uri());
            }
        }
    }

    @Test
    void nextPageIsNullOnTheLastPage() throws Exception {
        try (StubServer server = new StubServer(pageReply(3, 3))) {
            Page page = client(server).coins().list(Params.of());
            assertFalse(page.hasMore());
            assertNull(page.nextPage());
        }
    }

    @Test
    void iteratingAPageWalksEveryPageInOrder() throws Exception {
        try (StubServer server = new StubServer(pageReply(1, 3), pageReply(2, 3), pageReply(3, 3))) {
            Page page = client(server).coins().list(Params.of().set("per_page", 2));
            List<Integer> seen = new ArrayList<>();
            for (JsonNode row : page) {
                seen.add(row.get("id").asInt());
            }
            assertEquals(List.of(1, 2, 3, 4, 5, 6), seen);
            assertEquals(3, server.hits());
        }
    }

    @Test
    void envelopeWithoutMetaOrLinksIsASafeFinalPage() throws Exception {
        try (StubServer server = new StubServer(Reply.json(200, "{\"data\":[{\"id\":9}]}"))) {
            Page page = client(server).coins().list(Params.of());
            assertEquals(1, page.data().size());
            assertFalse(page.hasMore());
            assertNull(page.nextPage());
        }
    }

    @Test
    void nonArrayDataIsAnEmptyPage() throws Exception {
        try (StubServer server = new StubServer(Reply.json(200, "{\"data\":null,\"meta\":{\"current_page\":1,\"last_page\":1}}"))) {
            Page page = client(server).coins().list(Params.of());
            assertTrue(page.data().isEmpty());
            assertFalse(page.hasMore());
        }
    }
}
