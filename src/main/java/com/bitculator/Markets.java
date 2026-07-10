package com.bitculator;

import com.fasterxml.jackson.databind.JsonNode;

/** Markets, tickers, and trading pairs. */
public final class Markets extends Resource {

    Markets(HttpClient http) {
        super(http);
    }

    /** Markets (tickers) for a coin (paginated). Params: page, per_page, exchange, pair,
     * instrument, search, min_volume, max_volume, min_change, max_change, sort. */
    public Page coinMarkets(String slug, Params params) {
        return http.page("/api/v1/coins/" + enc(slug) + "/markets", params);
    }

    /** Trading symbols for a coin. */
    public JsonNode coinSymbols(String slug) {
        return http.get("/api/v1/coins/" + enc(slug) + "/symbols", null);
    }

    /** List tickers across the market (paginated). */
    public Page tickers(Params params) {
        return http.page("/api/v1/tickers", params);
    }

    /** List trading pairs (paginated). Params: page, per_page, search, coin, min_volume, max_volume, sort. */
    public Page pairs(Params params) {
        return http.page("/api/v1/pairs", params);
    }

    /** Get a single trading pair by id. */
    public JsonNode pair(Object id) {
        return http.get("/api/v1/pairs/" + enc(id), null);
    }
}
