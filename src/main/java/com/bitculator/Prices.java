package com.bitculator;

import com.fasterxml.jackson.databind.JsonNode;

/** Lightweight price lookups. */
public final class Prices extends Resource {

    Prices(HttpClient http) {
        super(http);
    }

    /** Get prices for many coins at once. Params: ids, slugs, symbols, convert. */
    public JsonNode list(Params params) {
        return http.get("/api/v1/prices", params);
    }

    /** Get the price of a single coin. Params: convert. */
    public JsonNode get(String slug, Params params) {
        return http.get("/api/v1/prices/" + enc(slug), params);
    }

    /** Historical price of a coin on a date. Params: slug, date. */
    public JsonNode historical(Params params) {
        return http.get("/api/v1/historical-price", params);
    }
}
