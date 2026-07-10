package com.bitculator;

import com.fasterxml.jackson.databind.JsonNode;

/** Exchanges: listings, trust score, history, markets, and listed assets. */
public final class Exchanges extends Resource {

    Exchanges(HttpClient http) {
        super(http);
    }

    /** List exchanges (paginated). Params: page, per_page, type, search, min_pairs, max_pairs,
     * min_assets, max_assets, min_volume, max_volume, ids, slugs, sort. */
    public Page list(Params params) {
        return http.page("/api/v1/exchanges", params);
    }

    /** Get a single exchange by slug. */
    public JsonNode get(String slug) {
        return http.get("/api/v1/exchanges/" + enc(slug), null);
    }

    /** Get an exchange's trust score. */
    public JsonNode trustScore(String slug) {
        return http.get("/api/v1/exchanges/" + enc(slug) + "/trust-score", null);
    }

    /** Volume/history for an exchange. Params: interval, start, end, limit. */
    public JsonNode history(String slug, Params params) {
        return http.get("/api/v1/exchanges/" + enc(slug) + "/history", params);
    }

    /** Sparkline for an exchange. Params: period. */
    public JsonNode sparkline(String slug, Params params) {
        return http.get("/api/v1/exchanges/" + enc(slug) + "/sparkline", params);
    }

    /** Markets (tickers) on an exchange (paginated). */
    public Page tickers(String slug, Params params) {
        return http.page("/api/v1/exchanges/" + enc(slug) + "/tickers", params);
    }

    /** Coins listed on an exchange (paginated). */
    public Page assets(String slug, Params params) {
        return http.page("/api/v1/exchanges/" + enc(slug) + "/assets", params);
    }
}
