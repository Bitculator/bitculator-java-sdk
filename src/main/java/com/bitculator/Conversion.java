package com.bitculator;

import com.fasterxml.jackson.databind.JsonNode;

/** Asset conversion and reference rates. */
public final class Conversion extends Resource {

    Conversion(HttpClient http) {
        super(http);
    }

    /** Convert an amount between two assets. Params: from, to, amount. */
    public JsonNode convert(Params params) {
        return http.get("/api/v1/convert", params);
    }

    /** List supported fiat currencies. */
    public JsonNode fiats() {
        return http.get("/api/v1/fiats", null);
    }

    /** List conversion rates. */
    public JsonNode rates() {
        return http.get("/api/v1/rates", null);
    }
}
