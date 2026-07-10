package com.bitculator;

import com.fasterxml.jackson.databind.JsonNode;

/** Service metadata: spec, health, and key usage. */
public final class Meta extends Resource {

    Meta(HttpClient http) {
        super(http);
    }

    /** The raw OpenAPI specification for the API. */
    public JsonNode openapi() {
        return http.get("/api/v1/openapi.json", null);
    }

    /** Health check. */
    public JsonNode ping() {
        return http.get("/api/v1/ping", null);
    }

    /** Current key usage and quota. */
    public JsonNode usage() {
        return http.get("/api/v1/usage", null);
    }
}
