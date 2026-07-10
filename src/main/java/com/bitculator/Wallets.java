package com.bitculator;

import com.fasterxml.jackson.databind.JsonNode;

/** Wallets: listings, release timeline, comparison, and supported coins. */
public final class Wallets extends Resource {

    Wallets(HttpClient http) {
        super(http);
    }

    /** List wallets (paginated). Params: page, per_page, search, min_score, max_score, tags, ids, slugs, sort. */
    public Page list(Params params) {
        return http.page("/api/v1/wallets", params);
    }

    /** Wallet release timeline (paginated). */
    public Page timeline(Params params) {
        return http.page("/api/v1/wallets/timeline", params);
    }

    /** Compare a set of wallets side by side. Params: slugs. */
    public JsonNode compare(Params params) {
        return http.get("/api/v1/wallets/compare", params);
    }

    /** Get a single wallet by slug. */
    public JsonNode get(String slug) {
        return http.get("/api/v1/wallets/" + enc(slug), null);
    }

    /** Coins supported by a wallet (paginated). */
    public Page assets(String slug, Params params) {
        return http.page("/api/v1/wallets/" + enc(slug) + "/assets", params);
    }
}
