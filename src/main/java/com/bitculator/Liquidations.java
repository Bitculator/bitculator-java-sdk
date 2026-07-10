package com.bitculator;

import com.fasterxml.jackson.databind.JsonNode;

/** Liquidation feed and aggregates. */
public final class Liquidations extends Resource {

    Liquidations(HttpClient http) {
        super(http);
    }

    /** Live liquidation feed (paginated). Params: page, per_page, exchange, instrument,
     * position, order, symbol, min_usd. */
    public Page feed(Params params) {
        return http.page("/api/v1/liquidations", params);
    }

    /** Hourly liquidation totals. Params: hours. */
    public JsonNode hourly(Params params) {
        return http.get("/api/v1/liquidations/hourly", params);
    }

    /** Daily liquidation totals. Params: days. */
    public JsonNode daily(Params params) {
        return http.get("/api/v1/liquidations/daily", params);
    }

    /** Today's liquidation summary. */
    public JsonNode summary() {
        return http.get("/api/v1/liquidations/summary", null);
    }

    /** Long/short liquidation netflow. Params: days. */
    public JsonNode netflow(Params params) {
        return http.get("/api/v1/liquidations/netflow", params);
    }

    /** Most-liquidated coins. Params: hours, limit. */
    public JsonNode coins(Params params) {
        return http.get("/api/v1/liquidations/coins", params);
    }
}
