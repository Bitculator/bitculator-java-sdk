package com.bitculator;

import java.util.Map;

/** Quota headers (X-Quota-*) from the most recent response. A field is null when the header was absent. */
public record Quota(Integer limit, Integer remaining, Integer reset, Map<String, String> raw) {
}
