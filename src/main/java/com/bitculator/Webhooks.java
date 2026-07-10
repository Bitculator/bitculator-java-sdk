package com.bitculator;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

/** Webhook endpoints and their delivery log. */
public final class Webhooks extends Resource {

    Webhooks(HttpClient http) {
        super(http);
    }

    /** List your webhook endpoints. */
    public JsonNode list() {
        return http.get("/api/v1/webhooks", null);
    }

    /** Register a webhook endpoint for a set of events. */
    public JsonNode create(String url, List<String> events) {
        return http.post("/api/v1/webhooks", Map.of("url", url, "events", events));
    }

    /** Delete a webhook endpoint by id. */
    public JsonNode delete(Object id) {
        return http.delete("/api/v1/webhooks/" + enc(id));
    }

    /** Send a test event to a webhook endpoint. */
    public JsonNode test(Object id) {
        return http.post("/api/v1/webhooks/" + enc(id) + "/test", null);
    }

    /** Delivery log for a webhook endpoint (paginated). Params: page, per_page. */
    public Page deliveries(Object id, Params params) {
        return http.page("/api/v1/webhooks/" + enc(id) + "/deliveries", params);
    }
}
