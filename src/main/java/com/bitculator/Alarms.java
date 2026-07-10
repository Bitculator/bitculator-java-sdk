package com.bitculator;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

/** Price alarms owned by the API key. */
public final class Alarms extends Resource {

    Alarms(HttpClient http) {
        super(http);
    }

    /** List your alarms (paginated). Params: page, per_page, status, direction, notification. */
    public Page list(Params params) {
        return http.page("/api/v1/alarms", params);
    }

    /** Create an alarm. */
    public JsonNode create(String name, String coin, String metric, String direction, double target, String notification) {
        return http.post("/api/v1/alarms", Map.of(
                "name", name,
                "coin", coin,
                "metric", metric,
                "direction", direction,
                "target", target,
                "notification", notification));
    }

    /** Delete an alarm by id. */
    public JsonNode delete(Object id) {
        return http.delete("/api/v1/alarms/" + enc(id));
    }
}
