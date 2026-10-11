package com.bitculator;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

/** Community sentiment and market-wide sentiment indices. */
public final class Sentiment extends Resource {

    Sentiment(HttpClient http) {
        super(http);
    }

    /** Community vote tallies for a coin. */
    public JsonNode votes(String slug) {
        return http.get("/api/v1/coins/" + enc(slug) + "/votes", null);
    }

    /** Cast a sentiment vote for a coin ("bullish" or "bearish"). */
    public JsonNode vote(String slug, String vote) {
        return http.post("/api/v1/coins/" + enc(slug) + "/votes", Map.of("vote", vote));
    }

    /** Fear &amp; Greed index. Params: coin. Each reading carries {@code index} (0-100, as shown on Bitculator; use it for display) and the raw {@code score} (-100..+100). */
    public JsonNode fearGreed(Params params) {
        return http.get("/api/v1/sentiment/fear-greed", params);
    }

    /** Bull / Bear index. Params: coin. */
    public JsonNode bullBear(Params params) {
        return http.get("/api/v1/sentiment/bull-bear", params);
    }

    /** Altseason index. Params: days. */
    public JsonNode altseason(Params params) {
        return http.get("/api/v1/sentiment/altseason", params);
    }

    /** Aggregate market indicator tally. */
    public JsonNode indicators() {
        return http.get("/api/v1/sentiment/indicators", null);
    }
}
