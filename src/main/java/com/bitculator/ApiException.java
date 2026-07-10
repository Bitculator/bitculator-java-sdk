package com.bitculator;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpHeaders;

/**
 * A non-2xx response, carrying the API's {@code { error: { code, message,
 * details } }} envelope. Use the {@code isXxx()} predicates to branch on status.
 */
public class ApiException extends BitculatorException {

    private final int status;
    private final String code;
    private final JsonNode details;
    private final String requestId;
    private final Integer retryAfter;

    public ApiException(int status, String code, String message, JsonNode details, String requestId, Integer retryAfter) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details;
        this.requestId = requestId;
        this.retryAfter = retryAfter;
    }

    public int status() {
        return status;
    }

    public String code() {
        return code;
    }

    public JsonNode details() {
        return details;
    }

    public String requestId() {
        return requestId;
    }

    /** Seconds to wait before retrying (429 only), or null. */
    public Integer retryAfter() {
        return retryAfter;
    }

    public boolean isAuth() {
        return status == 401;
    }

    public boolean isPermission() {
        return status == 403;
    }

    public boolean isNotFound() {
        return status == 404;
    }

    public boolean isValidation() {
        return status == 422;
    }

    public boolean isRateLimited() {
        return status == 429;
    }

    public boolean isServer() {
        return status >= 500;
    }

    static ApiException from(int status, JsonNode body, HttpHeaders headers) {
        String code = null;
        String message = "HTTP " + status;
        JsonNode details = null;

        if (body != null && body.isObject()) {
            JsonNode err = body.get("error");
            if (err != null && err.isObject()) {
                if (err.hasNonNull("code")) {
                    code = err.get("code").asText();
                }
                if (err.hasNonNull("message")) {
                    message = err.get("message").asText();
                }
                details = err.get("details");
            } else if (body.hasNonNull("message")) {
                message = body.get("message").asText();
            }
        }

        String requestId = headers.firstValue("x-request-id").orElse(null);
        Integer retryAfter = null;
        if (status == 429) {
            retryAfter = headers.firstValue("retry-after")
                    .flatMap(ApiException::tryParseInt)
                    .orElse(null);
        }
        return new ApiException(status, code, message, details, requestId, retryAfter);
    }

    private static java.util.Optional<Integer> tryParseInt(String s) {
        try {
            return java.util.Optional.of(Integer.parseInt(s.trim()));
        } catch (NumberFormatException e) {
            return java.util.Optional.empty();
        }
    }
}
