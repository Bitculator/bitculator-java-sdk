package com.bitculator;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URLEncoder;

/** Shared base for every resource group: holds the transport and encodes path segments. */
abstract class Resource {

    final HttpClient http;

    Resource(HttpClient http) {
        this.http = http;
    }

    /** Percent-encode a dynamic path segment (slug or id) so it is URL-safe. */
    static String enc(Object value) {
        return URLEncoder.encode(String.valueOf(value), UTF_8).replace("+", "%20");
    }
}
