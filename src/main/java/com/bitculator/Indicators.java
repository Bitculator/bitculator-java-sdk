package com.bitculator;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Technical indicators for a coin.
 *
 * <p>Period indicators (rsi, stochRsi, sma, cci, mfi, williamsR, priceVolatility,
 * volumeVolatility) accept: period, start, end, limit. Series indicators (macd,
 * obv, adx, vwap, cmf) accept: start, end, limit.
 */
public final class Indicators extends Resource {

    Indicators(HttpClient http) {
        super(http);
    }

    private JsonNode indicator(String slug, String name, Params params) {
        return http.get("/api/v1/coins/" + enc(slug) + "/indicators/" + name, params);
    }

    /** All indicators for a coin at a glance. */
    public JsonNode snapshot(String slug) {
        return http.get("/api/v1/coins/" + enc(slug) + "/indicators", null);
    }

    public JsonNode rsi(String slug, Params params) {
        return indicator(slug, "rsi", params);
    }

    public JsonNode stochRsi(String slug, Params params) {
        return indicator(slug, "stoch-rsi", params);
    }

    public JsonNode sma(String slug, Params params) {
        return indicator(slug, "sma", params);
    }

    public JsonNode cci(String slug, Params params) {
        return indicator(slug, "cci", params);
    }

    public JsonNode mfi(String slug, Params params) {
        return indicator(slug, "mfi", params);
    }

    public JsonNode williamsR(String slug, Params params) {
        return indicator(slug, "williams-r", params);
    }

    public JsonNode priceVolatility(String slug, Params params) {
        return indicator(slug, "price-volatility", params);
    }

    public JsonNode volumeVolatility(String slug, Params params) {
        return indicator(slug, "volume-volatility", params);
    }

    public JsonNode macd(String slug, Params params) {
        return indicator(slug, "macd", params);
    }

    public JsonNode obv(String slug, Params params) {
        return indicator(slug, "obv", params);
    }

    public JsonNode adx(String slug, Params params) {
        return indicator(slug, "adx", params);
    }

    public JsonNode vwap(String slug, Params params) {
        return indicator(slug, "vwap", params);
    }

    public JsonNode cmf(String slug, Params params) {
        return indicator(slug, "cmf", params);
    }
}
