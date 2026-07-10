package com.bitculator;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * One page of a paginated list, plus lazy forward navigation. {@link #data()} is
 * the current page's rows; iterating a Page (it is {@link Iterable}) walks every
 * following page. Rows are Jackson {@link JsonNode}s — convert with
 * {@code Bitculator.mapper().treeToValue(node, MyType.class)} if you want POJOs.
 */
public final class Page implements Iterable<JsonNode> {

    private final HttpClient http;
    private final String path;
    private final Params params;
    private final List<JsonNode> data;
    private final Meta meta;
    private final Links links;

    Page(HttpClient http, String path, Params params, JsonNode envelope) {
        this.http = http;
        this.path = path;
        this.params = params;
        List<JsonNode> rows = new ArrayList<>();
        if (envelope != null) {
            JsonNode array = envelope.get("data");
            if (array != null && array.isArray()) {
                array.forEach(rows::add);
            }
        }
        this.data = rows;
        this.meta = Meta.from(envelope == null ? null : envelope.get("meta"));
        this.links = Links.from(envelope == null ? null : envelope.get("links"));
    }

    public List<JsonNode> data() {
        return data;
    }

    public Meta meta() {
        return meta;
    }

    public Links links() {
        return links;
    }

    /** True if at least one more page exists. */
    public boolean hasMore() {
        if (links.next() != null && !links.next().isEmpty()) {
            return true;
        }
        return meta.currentPage() != 0 && meta.lastPage() != 0 && meta.currentPage() < meta.lastPage();
    }

    /** Fetch the next page, or null if this is the last one. */
    public Page nextPage() {
        if (!hasMore()) {
            return null;
        }
        return http.page(path, nextPageParams());
    }

    private Params nextPageParams() {
        return params.copy().set("page", meta.currentPage() + 1);
    }

    /** Iterating a Page walks all pages; use {@link #data()} for just this page. */
    @Override
    public Iterator<JsonNode> iterator() {
        return new AutoPager();
    }

    private final class AutoPager implements Iterator<JsonNode> {
        private Iterator<JsonNode> current = data.iterator();
        private Params next = hasMore() ? nextPageParams() : null;

        @Override
        public boolean hasNext() {
            if (current.hasNext()) {
                return true;
            }
            while (next != null) {
                Page page = http.page(path, next);
                next = page.hasMore() ? page.nextPageParams() : null;
                current = page.data.iterator();
                if (current.hasNext()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public JsonNode next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return current.next();
        }
    }

    /** Laravel paginator metadata. */
    public record Meta(int currentPage, Integer from, int lastPage, int perPage, Integer to, long total) {
        static Meta from(JsonNode n) {
            if (n == null || n.isNull()) {
                return new Meta(0, null, 0, 0, null, 0);
            }
            return new Meta(
                    n.path("current_page").asInt(0),
                    n.hasNonNull("from") ? n.get("from").asInt() : null,
                    n.path("last_page").asInt(0),
                    n.path("per_page").asInt(0),
                    n.hasNonNull("to") ? n.get("to").asInt() : null,
                    n.path("total").asLong(0));
        }
    }

    /** Page navigation links. */
    public record Links(String first, String last, String prev, String next) {
        static Links from(JsonNode n) {
            if (n == null || n.isNull()) {
                return new Links(null, null, null, null);
            }
            return new Links(text(n, "first"), text(n, "last"), text(n, "prev"), text(n, "next"));
        }

        private static String text(JsonNode n, String key) {
            return n.hasNonNull(key) ? n.get(key).asText() : null;
        }
    }
}
