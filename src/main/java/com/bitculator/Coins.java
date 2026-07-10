package com.bitculator;

import com.fasterxml.jackson.databind.JsonNode;

/** Coins &amp; tokens: listings, movers, detail, and history. */
public final class Coins extends Resource {

    Coins(HttpClient http) {
        super(http);
    }

    /** List coins (paginated). Params: page, per_page, type, status, search, min_price,
     * max_price, min_marketcap, max_marketcap, min_volume, max_volume, ids, slugs, symbols, sort, interval. */
    public Page list(Params params) {
        return http.page("/api/v1/coins", params);
    }

    public Page recentlyAdded(Params params) {
        return http.page("/api/v1/coins/recently-added", params);
    }

    public Page gainers(Params params) {
        return http.page("/api/v1/coins/gainers", params);
    }

    public Page losers(Params params) {
        return http.page("/api/v1/coins/losers", params);
    }

    /** Trending coins. Params: limit. */
    public JsonNode trending(Params params) {
        return http.get("/api/v1/coins/trending", params);
    }

    /** Get a single coin by slug. Params: locale. */
    public JsonNode get(String slug, Params params) {
        return http.get("/api/v1/coins/" + enc(slug), params);
    }

    /** OHLC candle history. Params: interval, start, end, limit. */
    public JsonNode history(String slug, Params params) {
        return http.get("/api/v1/coins/" + enc(slug) + "/history", params);
    }

    /** Marketcap history. Params: interval, start, end, limit. */
    public JsonNode marketcapHistory(String slug, Params params) {
        return http.get("/api/v1/coins/" + enc(slug) + "/marketcap-history", params);
    }

    /** Price sparkline. Params: period. */
    public JsonNode sparkline(String slug, Params params) {
        return http.get("/api/v1/coins/" + enc(slug) + "/sparkline", params);
    }
}
