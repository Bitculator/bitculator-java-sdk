package com.bitculator;

import com.fasterxml.jackson.databind.JsonNode;

/** Global market aggregates. */
public final class GlobalMarket extends Resource {

    GlobalMarket(HttpClient http) {
        super(http);
    }

    /** Global market snapshot (total marketcap, volume, dominance, ...). */
    public JsonNode snapshot() {
        return http.get("/api/v1/global", null);
    }

    /** Market heatmap data. */
    public JsonNode heatmap() {
        return http.get("/api/v1/global/heatmap", null);
    }

    /** Global marketcap or volume history. {@code metric} is e.g. "marketcap" or "volume". Params: period. */
    public JsonNode history(String metric, Params params) {
        return http.get("/api/v1/global/history/" + enc(metric), params);
    }
}
