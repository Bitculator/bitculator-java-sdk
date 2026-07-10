package com.bitculator;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A bag of query parameters. Keys mirror the API's names (snake_case); build it
 * fluently:
 *
 * <pre>{@code
 * Params.of().set("per_page", 50).set("sort", "-marketcap");
 * }</pre>
 */
public final class Params {

    private final LinkedHashMap<String, String> values = new LinkedHashMap<>();

    public Params() {
    }

    public static Params of() {
        return new Params();
    }

    /**
     * Set a parameter. A {@code null} value or empty collection is skipped;
     * collections/arrays become CSV; a repeated key replaces the previous value.
     */
    public Params set(String key, Object value) {
        if (value == null) {
            return this;
        }
        if (value instanceof Boolean b) {
            values.put(key, b ? "true" : "false");
        } else if (value instanceof Double || value instanceof Float) {
            // Render floating-point as a plain full-precision decimal (no scientific
            // notation like 1.0E-7 / 1.0E8) — the API accepts exponent form, but the
            // plain-decimal wire format is kept for consistency across the SDK family.
            String plain = new java.math.BigDecimal(value.toString())
                    .stripTrailingZeros()
                    .toPlainString();
            values.put(key, plain);
        } else if (value instanceof Iterable<?> iterable) {
            List<String> parts = new ArrayList<>();
            for (Object item : iterable) {
                if (item != null) {
                    parts.add(String.valueOf(item));
                }
            }
            if (!parts.isEmpty()) {
                values.put(key, String.join(",", parts));
            }
        } else if (value instanceof Object[] array) {
            List<String> parts = new ArrayList<>();
            for (Object item : array) {
                if (item != null) {
                    parts.add(String.valueOf(item));
                }
            }
            if (!parts.isEmpty()) {
                values.put(key, String.join(",", parts));
            }
        } else {
            values.put(key, String.valueOf(value));
        }
        return this;
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    Params copy() {
        Params p = new Params();
        p.values.putAll(values);
        return p;
    }

    String toQueryString() {
        if (values.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("?");
        boolean first = true;
        for (Map.Entry<String, String> e : values.entrySet()) {
            if (!first) {
                sb.append('&');
            }
            first = false;
            sb.append(URLEncoder.encode(e.getKey(), UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(e.getValue(), UTF_8));
        }
        return sb.toString();
    }
}
