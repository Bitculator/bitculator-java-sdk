package com.bitculator;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

/** Editorial content: articles, videos, and insights. */
public final class Editorial extends Resource {

    Editorial(HttpClient http) {
        super(http);
    }

    /** Videos for a coin (paginated). Params: page, per_page, type, search. */
    public Page coinVideos(String slug, Params params) {
        return http.page("/api/v1/coins/" + enc(slug) + "/videos", params);
    }

    /** Insight timeline for a coin. Params: locale, offset, limit. */
    public JsonNode coinInsights(String slug, Params params) {
        return http.get("/api/v1/coins/" + enc(slug) + "/insights", params);
    }

    /** List articles (paginated). Params: page, per_page, locale, tag, coin, exchange, wallet, search. */
    public Page articles(Params params) {
        return http.page("/api/v1/articles", params);
    }

    /** Get a single article by slug. Params: locale. */
    public JsonNode article(String slug, Params params) {
        return http.get("/api/v1/articles/" + enc(slug), params);
    }

    /** Submit helpful/unhelpful feedback on an article. */
    public JsonNode submitArticleFeedback(String slug, boolean helpful) {
        return http.post("/api/v1/articles/" + enc(slug) + "/feedback", Map.of("helpful", helpful));
    }

    /** Get a single video by id. */
    public JsonNode video(Object id) {
        return http.get("/api/v1/videos/" + enc(id), null);
    }

    /** List insights (paginated). Params: page, per_page, locale, type, coin, search, sort. */
    public Page insights(Params params) {
        return http.page("/api/v1/insights", params);
    }

    /** Get a single insight by id. Params: locale. */
    public JsonNode insight(Object id, Params params) {
        return http.get("/api/v1/insights/" + enc(id), params);
    }
}
