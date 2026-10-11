package com.bitculator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.GZIPInputStream;

/**
 * The DRY transport core. Every request in the SDK flows through send(): URL +
 * query building, auth headers, JSON (de)serialization, gzip response
 * decompression, the response envelope, quota capture, retries with backoff,
 * and error mapping. Uses the JDK's java.net.http client; the only third-party
 * dependency is Jackson for JSON.
 */
final class HttpClient {

    private final String apiKey;
    private final String baseUrl;
    private final String userAgent;
    private final int maxRetries;
    private final Duration timeout;
    private final java.net.http.HttpClient http;
    private final ObjectMapper mapper = JsonMapper.builder().build();

    private volatile Quota quota;

    HttpClient(String apiKey, String baseUrl, Duration timeout, int maxRetries, String userAgent,
               java.net.http.HttpClient client) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.timeout = timeout;
        this.maxRetries = maxRetries;
        this.userAgent = userAgent;
        this.http = client != null ? client : java.net.http.HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    ObjectMapper mapper() {
        return mapper;
    }

    Quota quota() {
        return quota;
    }

    JsonNode get(String path, Params params) {
        return unwrap(send("GET", path, params, null));
    }

    JsonNode post(String path, Object body) {
        return unwrap(send("POST", path, null, body));
    }

    JsonNode delete(String path) {
        return unwrap(send("DELETE", path, null, null));
    }

    Page page(String path, Params params) {
        return new Page(this, path, params, send("GET", path, params, null));
    }

    private JsonNode send(String method, String path, Params params, Object body) {
        String url = baseUrl + path + (params == null ? "" : params.toQueryString());
        // GET and DELETE are idempotent and safe to auto-retry; POST is not, so a
        // POST must never be re-sent on a network/timeout error or a 5xx response
        // (the server may have already processed it).
        boolean isIdempotent = "GET".equals(method) || "DELETE".equals(method);
        int attempt = 0;
        while (true) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Accept", "application/json")
                    // java.net.http never negotiates compression itself; readJson() gunzips.
                    .header("Accept-Encoding", "gzip")
                    .header("User-Agent", userAgent);

            if (body != null) {
                String json;
                try {
                    json = mapper.writeValueAsString(body);
                } catch (IOException e) {
                    throw new BitculatorException("Failed to encode request body: " + e.getMessage(), e);
                }
                builder.header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(json));
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }

            HttpResponse<byte[]> response;
            try {
                response = http.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            } catch (HttpTimeoutException e) {
                if (isIdempotent && attempt < maxRetries) {
                    sleep(backoff(attempt++));
                    continue;
                }
                throw new TimeoutException("Request to " + path + " timed out after " + timeout);
            } catch (IOException e) {
                if (isIdempotent && attempt < maxRetries) {
                    sleep(backoff(attempt++));
                    continue;
                }
                throw new ConnectionException("Network error requesting " + path + ": " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ConnectionException("Request to " + path + " was interrupted");
            }

            this.quota = parseQuota(response.headers());
            int status = response.statusCode();

            if (status >= 200 && status < 300) {
                // A 2xx body must be valid JSON — parse() throws the typed decode error if not.
                return parse(response);
            }
            // Error bodies parse leniently: a proxy's HTML-bodied 502/503 must still
            // reach the retry gate and ApiException mapping below, not throw a decode error.
            JsonNode json = parseLenient(response);
            if ((status == 429 || (status >= 500 && isIdempotent)) && attempt < maxRetries) {
                sleep(retryDelay(response.headers(), attempt++));
                continue;
            }
            throw ApiException.from(status, json, response.headers());
        }
    }

    // ── helpers (all pure; the single place these concerns are implemented) ──

    private JsonNode unwrap(JsonNode node) {
        if (node != null && node.isObject() && node.has("data")) {
            return node.get("data");
        }
        return node;
    }

    private JsonNode parse(HttpResponse<byte[]> response) {
        try {
            return readJson(response);
        } catch (IOException e) {
            throw new BitculatorException("Failed to decode response: " + e.getMessage(), e);
        }
    }

    private JsonNode parseLenient(HttpResponse<byte[]> response) {
        try {
            return readJson(response);
        } catch (IOException e) {
            return mapper.getNodeFactory().nullNode();
        }
    }

    /** The body as JSON (a null node when empty), gunzipped first when the server compressed it. */
    private JsonNode readJson(HttpResponse<byte[]> response) throws IOException {
        byte[] body = response.body();
        if (body != null && body.length > 0 && isGzip(response.headers())) {
            try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(body))) {
                body = in.readAllBytes();
            }
        }
        if (body == null || body.length == 0) {
            return mapper.getNodeFactory().nullNode();
        }
        return mapper.readTree(body);
    }

    private static boolean isGzip(HttpHeaders headers) {
        return headers.firstValue("content-encoding")
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> value.equals("gzip") || value.equals("x-gzip"))
                .isPresent();
    }

    private Quota parseQuota(HttpHeaders headers) {
        Map<String, String> raw = new HashMap<>();
        headers.map().forEach((key, list) -> {
            // Locale.ROOT: header matching must not depend on the default locale
            // (e.g. the Turkish dotless-i would break "X-QUOTA-*" lookups).
            String lower = key.toLowerCase(Locale.ROOT);
            if (lower.startsWith("x-quota-") && !list.isEmpty()) {
                raw.put(lower, list.get(0));
            }
        });
        return new Quota(
                num(raw.get("x-quota-limit")),
                num(raw.get("x-quota-remaining")),
                num(raw.get("x-quota-reset")),
                Map.copyOf(raw));
    }

    private static Integer num(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Duration retryDelay(HttpHeaders headers, int attempt) {
        Integer seconds = headers.firstValue("retry-after").map(HttpClient::num).orElse(null);
        if (seconds != null && seconds > 0) {
            // Honor Retry-After but cap it at 60s: a misbehaving proxy must not
            // block a synchronous call for an hour.
            return Duration.ofSeconds(Math.min(seconds, 60));
        }
        return backoff(attempt);
    }

    private Duration backoff(int attempt) {
        // 250ms, 500ms, 1s, 2s, 4s, then 8s flat; clamping the shift keeps the
        // math safe for any configured maxRetries (a << past 63 would wrap).
        long ms = 250L * (1L << Math.min(attempt, 5));
        return Duration.ofMillis(ms + ThreadLocalRandom.current().nextInt(100));
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConnectionException("Retry sleep interrupted");
        }
    }
}
